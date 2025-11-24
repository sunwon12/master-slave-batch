package com.example.demo.batch.scheduler;

import com.example.demo.entity.AuctionStatus;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AuctionEndSchedulerPerformanceTest {

    private static final int TEST_DATA_COUNT = 200_000;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job auctionEndJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    private Runtime runtime;
    private HikariPoolMXBean poolMXBean;

    @BeforeEach
    void setUp() {
        // 데이터 삽입 속도 때문에 SQL 직접 실행으로, SQL문 위치: resources/test-dat/data.sql

        // 배치 메타데이터 초기화 (이전 테스트 기록 삭제)
        jdbcTemplate.execute("DELETE FROM BATCH_STEP_EXECUTION_CONTEXT");
        jdbcTemplate.execute("DELETE FROM BATCH_STEP_EXECUTION");
        jdbcTemplate.execute("DELETE FROM BATCH_JOB_EXECUTION_CONTEXT");
        jdbcTemplate.execute("DELETE FROM BATCH_JOB_EXECUTION_PARAMS");
        jdbcTemplate.execute("DELETE FROM BATCH_JOB_EXECUTION");
        jdbcTemplate.execute("DELETE FROM BATCH_JOB_INSTANCE");

        runtime = Runtime.getRuntime();

        // HikariCP 커넥션 풀 정보 초기화
        if (dataSource instanceof HikariDataSource) {
            poolMXBean = ((HikariDataSource) dataSource).getHikariPoolMXBean();
        }
    }


    @Test
    @DisplayName("20만건 경매 종료 배치 Job 상세 성능 분석")
    void measureDetailedPerformance() throws Exception {
        // Given - 테스트 데이터 생성
        // currentTime을 미래로 설정하여 endTime <= currentTime 조건 충족
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("currentTime", LocalDateTime.now().plusDays(1).toString())
                .addLong("runId", System.currentTimeMillis())  // 고유 ID로 Job 중복 방지
                .toJobParameters();

        System.gc();
        Thread.sleep(100);

        long initialMemory = getUsedMemory();
        final long[] peakMemory = {initialMemory};

        // 별도 스레드에서 메모리 모니터링
        Thread memoryMonitor = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    long currentMemory = getUsedMemory();
                    synchronized (peakMemory) {
                        if (currentMemory > peakMemory[0]) {
                            peakMemory[0] = currentMemory;
                        }
                    }
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        long startTime = System.currentTimeMillis();
        memoryMonitor.start();

        // When
        JobExecution jobExecution = jobLauncher.run(auctionEndJob, jobParameters);

        memoryMonitor.interrupt();
        memoryMonitor.join(1000);
        long endTime = System.currentTimeMillis();
        long finalMemory = getUsedMemory();

        // 처리된 데이터 수 확인
        long processedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auctions WHERE status = ?",
                Long.class,
                AuctionStatus.ENDED.name()
        );
        long executionTimeMs = endTime - startTime;
        double executionTimeSec = executionTimeMs / 1000.0;

        // 결과 출력
        System.out.println("========== 상세 성능 분석 결과 (20만건) ==========");
        System.out.println("Job 상태: " + jobExecution.getStatus());
        System.out.println("Job 시작 시간: " + jobExecution.getStartTime());
        System.out.println("Job 종료 시간: " + jobExecution.getEndTime());
        System.out.println("-------------------------------------------");
        System.out.println("처리된 데이터 수: " + processedCount);
        System.out.println("총 실행 시간: " + executionTimeMs + " ms (" + String.format("%.2f", executionTimeSec) + " 초)");
        System.out.println("-------------------------------------------");
        System.out.println("메모리 사용량:");
        System.out.println("  - 초기 메모리: " + formatMemorySize(initialMemory));
        System.out.println("  - 최대 메모리: " + formatMemorySize(peakMemory[0]));
        System.out.println("  - 최종 메모리: " + formatMemorySize(finalMemory));
        System.out.println("  - 최대 증가량: " + formatMemorySize(peakMemory[0] - initialMemory));
        System.out.println("-------------------------------------------");
        System.out.println("JVM 메모리 정보:");
        System.out.println("  - JVM 최대 메모리: " + formatMemorySize(runtime.maxMemory()));
        System.out.println("  - JVM 총 메모리: " + formatMemorySize(runtime.totalMemory()));
        System.out.println("  - JVM 사용 가능 메모리: " + formatMemorySize(runtime.freeMemory()));
        System.out.println("-------------------------------------------");
        System.out.println("성능 지표:");
        System.out.println("  - 처리량: " + String.format("%.2f", processedCount / executionTimeSec) + " 건/초");
        System.out.println("  - 건당 처리 시간: " + String.format("%.4f", executionTimeMs / (double) processedCount) + " ms");
        System.out.println("  - 1만건당 처리 시간: " + String.format("%.2f", (executionTimeMs / (double) processedCount) * 10000) + " ms");
        System.out.println("-------------------------------------------");

        // 검증
        assertThat(jobExecution.getStatus().isUnsuccessful()).isFalse();
        assertThat(processedCount).isEqualTo(TEST_DATA_COUNT);
    }

    private long getUsedMemory() {
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private String formatMemorySize(long bytes) {
        if (bytes < 0) {
            return "-" + formatMemorySize(-bytes);
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        }
        if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        }
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}

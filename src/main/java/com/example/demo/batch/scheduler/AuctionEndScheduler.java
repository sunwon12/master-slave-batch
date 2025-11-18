package com.example.demo.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;


@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionEndScheduler {

    private final JobLauncher jobLauncher;
    private final Job auctionEndJob;


    @Scheduled(cron = "0 * * * * *")  // 매분 0초에 실행
    public void runAuctionEndJob() {
        try {
            log.info("========== 경매 종료 배치 스케줄 시작: {} ==========", LocalDateTime.now());

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("executionTime", LocalDateTime.now())
                    .toJobParameters();

            jobLauncher.run(auctionEndJob, jobParameters);

            log.info("========== 경매 종료 배치 스케줄 완료: {} ==========", LocalDateTime.now());

        } catch (JobExecutionAlreadyRunningException e) {
            log.warn("이전 배치가 아직 실행 중입니다. 다음 스케줄까지 대기합니다.", e);

        } catch (JobRestartException e) {
            log.error("배치 재시작 중 오류가 발생했습니다.", e);

        } catch (JobInstanceAlreadyCompleteException e) {
            log.warn("이미 완료된 Job 인스턴스입니다. (이 경우는 JobParameters가 중복되었을 때 발생)", e);

        } catch (JobParametersInvalidException e) {
            log.error("잘못된 Job 파라미터입니다.", e);

        } catch (Exception e) {
            log.error("예상치 못한 오류가 발생했습니다.", e);
        }
    }
}

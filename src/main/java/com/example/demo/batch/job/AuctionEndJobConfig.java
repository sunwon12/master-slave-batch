package com.example.demo.batch.job;

import com.example.demo.dto.AuctionEndDto;
import com.example.demo.entity.AuctionStatus;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.Map;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AuctionEndJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final DataSource dataSource;

    private static final int CHUNK_SIZE = 1000;

    @Bean
    public Job auctionEndJob() {
        log.info("========== 경매 종료 Job 설정 ==========");
        return new JobBuilder("auctionEndJob", jobRepository)
                .start(auctionEndStep())
                .preventRestart()
                .build();
    }

    @Bean
    public Step auctionEndStep() {
        log.info("========== 경매 종료 Step 설정 ==========");
        return new StepBuilder("auctionEndStep", jobRepository)
                .<AuctionEndDto, AuctionEndDto>chunk(CHUNK_SIZE, transactionManager)
                .reader(expiredAuctionReader(null))
                .writer(auctionWriter())
                .build();
    }


    @Bean
    @StepScope
    public JpaPagingItemReader<AuctionEndDto> expiredAuctionReader(
            @Value("#{jobParameters['currentTime']}") String currentTime) {
        log.info("========== Reader 설정: 만료된 경매 조회 (DTO Projection) ==========");

        JpaPagingItemReader<AuctionEndDto> reader = new JpaPagingItemReader<>() {
            @Override
            public int getPage() {
                return 0;  // 항상 첫 번째 페이지 읽기
            }
        };

        reader.setName("expiredAuctionReader");
        reader.setEntityManagerFactory(entityManagerFactory);
        reader.setPageSize(CHUNK_SIZE);
        reader.setQueryString("SELECT new com.example.demo.dto.AuctionEndDto(a.id) " +
                "FROM Auction a " +
                "WHERE a.status = :status " +
                "AND a.endTime <= :currentTime " +
                "ORDER BY a.id ASC");
        reader.setParameterValues(Map.of(
                "status", AuctionStatus.ACTIVE,
                "currentTime", LocalDateTime.parse(currentTime)
        ));

        return reader;
    }

    @Bean
    @StepScope
    public JdbcBatchItemWriter<AuctionEndDto> auctionWriter() {
        log.info("========== Writer 설정: JDBC 배치 업데이트 ==========");

        return new JdbcBatchItemWriterBuilder<AuctionEndDto>()
                .dataSource(dataSource)
                .sql("UPDATE auctions SET status = 'ENDED' WHERE id = :id")
                .beanMapped()
                .build();
    }
}

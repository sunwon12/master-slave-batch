package com.example.demo.batch.job;

import com.example.demo.entity.Auction;
import com.example.demo.entity.AuctionStatus;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AuctionEndJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    private static final int CHUNK_SIZE = 100;

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
                .<Auction, Auction>chunk(CHUNK_SIZE, transactionManager)
                .reader(expiredAuctionReader())
                .processor(auctionEndProcessor())
                .writer(auctionWriter())
                .build();
    }


    @Bean
    public JpaPagingItemReader<Auction> expiredAuctionReader() {
        log.info("========== Reader 설정: 만료된 경매 조회 ==========");

        return new JpaPagingItemReaderBuilder<Auction>()
                .name("expiredAuctionReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .queryString("SELECT a FROM Auction a " +
                        "WHERE a.status = :status " +
                        "AND a.endTime <= :currentTime " +
                        "ORDER BY a.endTime ASC")
                .parameterValues(Map.of(
                        "status", AuctionStatus.ACTIVE,
                        "currentTime", LocalDateTime.now()
                ))
                .build();
    }


    @Bean
    public ItemProcessor<Auction, Auction> auctionEndProcessor() {
        log.info("========== Processor 설정: 경매 상태 변경 로직 ==========");

        return auction -> {
            log.debug("경매 종료 처리 - ID: {}, Title: {}, EndTime: {}",
                    auction.getId(), auction.getTitle(), auction.getEndTime());
            auction.setStatus(AuctionStatus.ENDED);
            return auction;
        };
    }

    @Bean
    public JpaItemWriter<Auction> auctionWriter() {
        log.info("========== Writer 설정: 경매 엔티티 저장 ==========");

        return new JpaItemWriterBuilder<Auction>()
                .entityManagerFactory(entityManagerFactory)
                .usePersist(false)
                .build();
    }
}

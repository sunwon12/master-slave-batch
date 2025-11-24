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
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
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
    public Job auctionEndJob() throws Exception {
        log.info("========== 경매 종료 Job 설정 ==========");
        return new JobBuilder("auctionEndJob", jobRepository)
                .start(auctionEndStep())
                .preventRestart()
                .build();
    }

    @Bean
    public Step auctionEndStep() throws Exception {
        log.info("========== 경매 종료 Step 설정 ==========");
        return new StepBuilder("auctionEndStep", jobRepository)
                .<AuctionEndDto, AuctionEndDto>chunk(CHUNK_SIZE, transactionManager)
                .reader(expiredAuctionJdbcReader(null))
                .writer(auctionWriter())
                .build();
    }


    @Bean
    @StepScope
    public JdbcPagingItemReader<AuctionEndDto> expiredAuctionJdbcReader(
            @Value("#{jobParameters['currentTime']}") String currentTime) throws Exception {

        log.info("========== Reader 설정: 만료된 경매 조회 (JDBC Paging) ==========");

        SqlPagingQueryProviderFactoryBean queryProvider = new SqlPagingQueryProviderFactoryBean();
        queryProvider.setDataSource(dataSource);
        queryProvider.setSelectClause("a.id");
        queryProvider.setFromClause("from auctions a");
        queryProvider.setWhereClause("where a.status = :status AND a.end_time <= :currentTime");
        queryProvider.setSortKey("id");

        return new JdbcPagingItemReaderBuilder<AuctionEndDto>()
                .name("expiredAuctionJdbcReader")
                .pageSize(CHUNK_SIZE)
                .dataSource(dataSource)
                .rowMapper(new BeanPropertyRowMapper<>(AuctionEndDto.class)) // DTO 필드(id)에 맞춰 자동 매핑
                .queryProvider(queryProvider.getObject())
                .parameterValues(Map.of(
                        "status", AuctionStatus.ACTIVE.name(),
                        "currentTime", LocalDateTime.parse(currentTime)
                ))
                .build();
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

package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class RoutingDataSource extends AbstractRoutingDataSource {

    private static final Logger logger = LoggerFactory.getLogger(RoutingDataSource.class);

    @Override
    protected Object determineCurrentLookupKey() {
        boolean isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        String dataSourceType = isReadOnly ? "slave" : "master";

        // 트랜잭션 상태 로깅
        boolean isTransactionActive = TransactionSynchronizationManager.isActualTransactionActive();
        String currentTransactionName = TransactionSynchronizationManager.getCurrentTransactionName();
        logger.info("🔀 [{}] 데이터소스 라우팅 결정:", Thread.currentThread().getName());
        logger.info("   → 대상 DB: {} (읽기전용={}, 트랜잭션활성={}, 트랜잭션명={})",
                    dataSourceType, isReadOnly, isTransactionActive, currentTransactionName);
        logger.info("   → 호출 위치: {}", getCurrentMethodInfo());

        return dataSourceType;
    }

    private String getCurrentMethodInfo() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // 현재 메소드에서 호출한 서비스 메소드 찾기
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            if (className.contains("com.example.demo.service") ||
                className.contains("com.example.demo.controller")) {
                return element.getClassName() + "." + element.getMethodName() + ":" + element.getLineNumber();
            }
        }
        return "알 수 없음";
    }
}

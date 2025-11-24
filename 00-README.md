# 경매 시스템 - 마스터-슬레이브 & Spring Batch 학습 프로젝트

## 프로젝트 개요

이 프로젝트는 **마스터-슬레이브 데이터베이스 아키텍처**와 **Spring Batch를 활용한 대용량 배치 처리**를 학습하기 위한 실전 경매 시스템입니다.

읽기 작업이 많은 경매 시스템의 특성을 활용하여 읽기/쓰기 분리(Read-Write Splitting)를 구현하고, 경매 종료 처리와 같은 대량 업데이트 작업을 Spring Batch로 안정적으로 처리합니다.

### 핵심 학습 목표

#### 마스터-슬레이브 아키텍처
1. Spring Boot에서 마스터-슬레이브 데이터베이스 구성
2. `@Transactional(readOnly = true/false)`를 통한 자동 라우팅
3. MySQL Replication 설정 및 관리
4. 읽기/쓰기 분리를 통한 성능 최적화
5. 복제 지연(Replication Lag) 이해 및 대응

#### Spring Batch
1. 대용량 데이터 배치 처리 (경매 종료 자동화)
2. Chunk 지향 처리 (Reader-Processor-Writer 패턴)
3. JdbcPagingItemReader를 사용한 페이징 읽기
4. Late Binding을 통한 동적 파라미터 전달
5. Spring Scheduler와 Batch 연동

## 기술 스택

- **Framework**: Spring Boot 3.5.6
- **Language**: Java 21
- **Database**: MySQL 8.0 (Master-Slave Replication)
- **ORM**: Spring Data JPA
- **Batch**: Spring Batch
- **Scheduler**: Spring Scheduler (@Scheduled)
- **Connection Pool**: HikariCP
- **Build Tool**: Gradle
- **Containerization**: Docker, Docker Compose

## 아키텍처

### 마스터-슬레이브 구조

```
┌─────────────────────┐
│  Spring Boot App    │
│   (Port: 8081)      │
└──────────┬──────────┘
           │
           ├─── @Transactional (쓰기) ──→ Master DB (Port: 3306)
           │                                  │
           │                            Replication (GTID)
           │                                  ↓
           └─── @Transactional(readOnly=true) ──→ Slave DB (Port: 3307)
```

### 배치 처리 아키텍처

```
┌────────────────────────────────────────────────────────┐
│            Spring Scheduler (매 분 실행)                │
└─────────────────┬──────────────────────────────────────┘
                  │
                  ▼
┌────────────────────────────────────────────────────────┐
│              Spring Batch Job (auctionEndJob)          │
├────────────────────────────────────────────────────────┤
│  Step: auctionEndStep (Chunk Size: 1000)              │
│                                                        │
│  ┌──────────────────────────────────────────────┐    │
│  │ Reader: JdbcPagingItemReader                 │    │
│  │ - 종료 시간이 지난 ACTIVE 경매 조회           │    │
│  │ - SQL 기반으로 필요한 데이터만 조회            │    │
│  │ - Late Binding으로 현재 시간 주입             │    │
│  └────────────┬─────────────────────────────────┘    │
│               │                                        │
│               ▼                                        │
│  ┌──────────────────────────────────────────────┐    │
│  │ Writer: JdbcBatchItemWriter                  │    │
│  │ - Chunk 단위로 ENDED 상태로 일괄 업데이트      │    │
│  │ - JDBC 배치 업데이트로 성능 최적화            │    │
│  └──────────────────────────────────────────────┘    │
└─────────────────┬──────────────────────────────────────┘
                  │
                  ▼
            Master DB (Port: 3306)
```

## 데이터 모델

### Entity 관계도
```
User (사용자)
  ↓ (1:N)
Auction (경매) ─── (1:N) ──→ Bid (입찰)
  ↓ (seller_id)              ↓ (bidder_id)
User (판매자)              User (입찰자)
```



## 주요 API 엔드포인트

### 사용자 API
- `POST /api/users` - 사용자 등록 (쓰기)
- `GET /api/users/{id}` - 사용자 조회 (읽기)
- `GET /api/users/email/{email}` - 이메일로 사용자 조회 (읽기)

### 경매 API
- `POST /api/auctions` - 경매 등록 (쓰기)
- `GET /api/auctions` - 경매 목록 조회 (읽기)
- `GET /api/auctions/{id}` - 경매 상세 조회 (읽기)
- `GET /api/auctions/seller/{sellerId}` - 판매자별 경매 (읽기)
- `PATCH /api/auctions/{id}/end` - 경매 종료 (쓰기)
- `POST /api/auctions/update-statuses` - 경매 상태 자동 업데이트 (쓰기)

### 입찰 API
- `POST /api/bids` - 입찰하기 (쓰기)
- `GET /api/bids/auctions/{auctionId}` - 경매별 입찰 내역 (읽기)
- `GET /api/bids/auctions/{auctionId}/recent` - 최근 입찰 10개 (읽기)
- `GET /api/bids/users/{userId}` - 사용자별 입찰 내역 (읽기)
- `GET /api/bids/auctions/{auctionId}/winning` - 현재 최고가 입찰 (읽기)
- `GET /api/bids/auctions/{auctionId}/statistics` - 입찰 통계 (읽기)

## 배치 작업

### 경매 종료 배치 (auctionEndJob)
- **실행 주기**: 매 분 (Cron: 0 * * * * *)
- **처리 대상**: 종료 시간이 지났지만 상태가 ACTIVE인 경매
- **Chunk Size**: 1000건
- **Reader**: JdbcPagingItemReader
  - SQL 기반으로 필요한 데이터만 조회하여 메모리 최적화
  - Late Binding으로 스케줄러 실행 시점의 시간을 동적으로 주입
  - JDBC 페이징으로 효율적인 데이터 조회
- **Writer**: JdbcBatchItemWriter
  - JDBC 배치 업데이트로 성능 최적화
  - Chunk 단위로 커밋하여 트랜잭션 분산

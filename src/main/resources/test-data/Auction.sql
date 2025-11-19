-- 1. 테스트 사용자 생성
INSERT INTO users (email, username, phone_number, created_at, updated_at)
VALUES ('test@example.com', 'testUser', '010-1234-5678', NOW(), NOW());

-- 2. 방금 생성한 user의 ID 확인
SELECT LAST_INSERT_ID() AS test_user_id;
-- 이 값을 아래 쿼리의 @testUserId에 사용


-- 1. 외래키 체크 해제 (잠시 보호막 끄기)
SET FOREIGN_KEY_CHECKS = 0;

-- 2. 테이블 초기화
TRUNCATE TABLE auctions;

-- 3. 외래키 체크 다시 설정 (보호막 켜기)
SET FOREIGN_KEY_CHECKS = 1;

DROP PROCEDURE IF EXISTS insert_auction_bulk;

DELIMITER $$

CREATE PROCEDURE insert_auction_bulk()
BEGIN
    DECLARE batch INT DEFAULT 0;
    DECLARE start_time_var DATETIME;
    DECLARE end_time_var DATETIME;

    -- 재귀 호출 깊이 제한 해제 (1000개 이상 만들 때 필요할 수 있음)
    SET @@cte_max_recursion_depth = 2000;

    SET start_time_var = NOW() - INTERVAL 1 DAY;
    SET end_time_var = NOW();

    WHILE batch < 200 DO
        -- 로그 (필요 시 주석 해제)
        -- SELECT CONCAT('Batch processing: ', batch + 1);

            INSERT INTO auctions (
                title, description, starting_price, current_price, min_bid_increment,
                seller_id, status, start_time, end_time, bid_count,
                winner_id, created_at, updated_at
            )
            -- [핵심 변경] WITH RECURSIVE를 사용하여 1부터 1000까지 숫자 생성
            WITH RECURSIVE dummy_rows AS (
                SELECT 1 AS n
                UNION ALL
                SELECT n + 1 FROM dummy_rows WHERE n < 1000
            )
SELECT
    CONCAT('Auction ', UUID()),    -- title
    'Dummy auction description',   -- description
    100.00,                        -- starting_price
    100.00,                        -- current_price
    10.00,                         -- min_bid_increment
    3,                             -- seller_id (존재하는 ID여야 함)
    'ACTIVE',                     -- status
    start_time_var,
    end_time_var,
    0,                             -- bid_count
    NULL,                          -- winner_id
    NOW(),
    NOW()
FROM dummy_rows; -- 여기서 1000개의 행이 나옵니다.

SET batch = batch + 1;
END WHILE;
END $$

DELIMITER ;

-- 실행
CALL insert_auction_bulk();

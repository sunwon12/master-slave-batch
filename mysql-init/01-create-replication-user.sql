-- Master DB 초기화 스크립트
CREATE USER IF NOT EXISTS 'replicator'@'%' IDENTIFIED BY 'replicator_password';
GRANT REPLICATION SLAVE ON *.* TO 'replicator'@'%';
GRANT SELECT ON *.* TO 'replicator'@'%';
FLUSH PRIVILEGES;

-- 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS master_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS slave_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 데모 사용자 생성
CREATE USER IF NOT EXISTS 'demo_user'@'%' IDENTIFIED BY 'demo_password';
GRANT ALL PRIVILEGES ON master_db.* TO 'demo_user'@'%';
GRANT ALL PRIVILEGES ON slave_db.* TO 'demo_user'@'%';
FLUSH PRIVILEGES;

-- Master 상태 확인용 뷰 생성
USE master_db;
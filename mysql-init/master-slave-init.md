# 컨테이너 완전 삭제 후 재시작
docker-compose down -v
docker-compose up -d

# 컨테이너가 준비될 때까지 대기
sleep 30

# replicator 계정 설정
docker exec mysql-master mysql -u root -ppassword -e "
ALTER USER 'replicator'@'%' IDENTIFIED WITH mysql_native_password BY 'replicator_password';
GRANT REPLICATION SLAVE ON *.* TO 'replicator'@'%';
FLUSH PRIVILEGES;
"

# 복제 설정
docker exec -i mysql-slave mysql -u root -ppassword <<'EOF'
CHANGE MASTER TO
MASTER_HOST='mysql-master',
MASTER_USER='replicator',
MASTER_PASSWORD='replicator_password',
MASTER_PORT=3306,
MASTER_AUTO_POSITION=1;
START SLAVE;
EOF

# 상태 확인
docker exec mysql-slave mysql -u root -ppassword -e "SHOW SLAVE STATUS\G" | grep -E "Slave_IO_Running|Slave_SQL_Running|Seconds_Behind"

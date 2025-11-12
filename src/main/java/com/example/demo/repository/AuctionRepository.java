package com.example.demo.repository;

import com.example.demo.entity.Auction;
import com.example.demo.entity.AuctionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {

    // 상태별 경매 조회
    List<Auction> findByStatus(AuctionStatus status);

    // 페이징을 지원하는 상태별 경매 조회
    Page<Auction> findByStatus(AuctionStatus status, Pageable pageable);

    // 종료 시간이 특정 시간 이전인 특정 상태의 경매
    List<Auction> findByStatusAndEndTimeBefore(AuctionStatus status, LocalDateTime time);

    // 시작 시간이 특정 시간 이전이고 특정 상태인 경매
    List<Auction> findByStatusAndStartTimeBefore(AuctionStatus status, LocalDateTime time);

    // 판매자별 경매 조회
    List<Auction> findBySellerId(Long sellerId);

    // 판매자별 경매 조회 (페이징)
    Page<Auction> findBySellerId(Long sellerId, Pageable pageable);
}

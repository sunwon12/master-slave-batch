package com.example.demo.repository;

import com.example.demo.entity.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    // 경매별 입찰 내역 (최신순)
    List<Bid> findByAuctionIdOrderByBidTimeDesc(Long auctionId);

    // 경매별 입찰 내역 (페이징, 최신순)
    Page<Bid> findByAuctionIdOrderByBidTimeDesc(Long auctionId, Pageable pageable);

    // 사용자별 입찰 내역 (페이징, 최신순)
    Page<Bid> findByBidderIdOrderByBidTimeDesc(Long bidderId, Pageable pageable);

    // 경매의 현재 최고가 입찰 (is_winning = true인 입찰)
    Optional<Bid> findFirstByAuctionIdAndIsWinningTrue(Long auctionId);

    // 경매의 최고가 입찰
    Optional<Bid> findFirstByAuctionIdOrderByBidAmountDesc(Long auctionId);

    // 경매의 입찰 수
    long countByAuctionId(Long auctionId);

    // 경매의 참여자 수 (중복 제거)
    @Query("SELECT COUNT(DISTINCT b.bidder.id) FROM Bid b WHERE b.auction.id = :auctionId")
    long countDistinctBiddersByAuctionId(@Param("auctionId") Long auctionId);
}

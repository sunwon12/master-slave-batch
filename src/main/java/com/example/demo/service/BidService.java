package com.example.demo.service;

import com.example.demo.dto.BidCreateRequest;
import com.example.demo.entity.Auction;
import com.example.demo.entity.AuctionStatus;
import com.example.demo.entity.Bid;
import com.example.demo.entity.User;
import com.example.demo.exception.*;
import com.example.demo.repository.AuctionRepository;
import com.example.demo.repository.BidRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BidService {

    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;

    /**
     * 입찰하기 - 쓰기 작업 (Master DB 사용)
     */
    @Transactional
    public Bid placeBid(BidCreateRequest request) {
        log.info("입찰 시도: 경매ID={}, 입찰자ID={}, 금액={}",
                request.getAuctionId(), request.getBidderId(), request.getBidAmount());

        // 엔티티 조회
        Auction auction = auctionRepository.findById(request.getAuctionId())
                .orElseThrow(() -> new AuctionNotFoundException(request.getAuctionId()));

        User bidder = userRepository.findById(request.getBidderId())
                .orElseThrow(() -> new UserNotFoundException(request.getBidderId()));

        // 비즈니스 검증
        validateBid(auction, bidder, request.getBidAmount());

        // 이전 최고가 입찰의 is_winning을 false로 변경
        bidRepository.findFirstByAuctionIdAndIsWinningTrue(auction.getId())
                .ifPresent(previousWinning -> {
                    previousWinning.setIsWinning(false);
                    log.info("이전 최고가 입찰 해제: 입찰ID={}", previousWinning.getId());
                });

        // 새 입찰 생성
        Bid bid = Bid.builder()
                .auction(auction)
                .bidder(bidder)
                .bidAmount(request.getBidAmount())
                .isWinning(true)
                .build();

        // 경매 정보 업데이트
        auction.setCurrentPrice(request.getBidAmount());
        auction.setBidCount(auction.getBidCount() + 1);

        Bid savedBid = bidRepository.save(bid);
        log.info("입찰 성공: 입찰ID={}", savedBid.getId());

        return savedBid;
    }

    /**
     * 경매별 입찰 내역 조회 (페이징) - 읽기 작업 (Slave DB 사용)
     */
    @Transactional(readOnly = true)
    public Page<Bid> getBidsByAuction(Long auctionId, Pageable pageable) {
        log.info("경매별 입찰 내역 조회: 경매ID={}", auctionId);
        return bidRepository.findByAuctionIdOrderByBidTimeDesc(auctionId, pageable);
    }

    /**
     * 경매별 입찰 내역 조회 (전체, 최근 10개) - 읽기 작업 (Slave DB 사용)
     */
    @Transactional(readOnly = true)
    public List<Bid> getRecentBidsByAuction(Long auctionId) {
        log.info("최근 입찰 내역 조회: 경매ID={}", auctionId);
        List<Bid> bids = bidRepository.findByAuctionIdOrderByBidTimeDesc(auctionId);
        return bids.stream().limit(10).toList();
    }

    /**
     * 사용자별 입찰 내역 조회 (페이징) - 읽기 작업 (Slave DB 사용)
     */
    @Transactional(readOnly = true)
    public Page<Bid> getBidsByUser(Long userId, Pageable pageable) {
        log.info("사용자별 입찰 내역 조회: 사용자ID={}", userId);
        return bidRepository.findByBidderIdOrderByBidTimeDesc(userId, pageable);
    }

    /**
     * 경매의 현재 최고가 입찰 조회 - 읽기 작업 (Slave DB 사용)
     */
    @Transactional(readOnly = true)
    public Bid getCurrentWinningBid(Long auctionId) {
        log.info("현재 최고가 입찰 조회: 경매ID={}", auctionId);
        return bidRepository.findFirstByAuctionIdAndIsWinningTrue(auctionId)
                .orElse(null);
    }

    /**
     * 경매의 입찰 통계 조회 - 읽기 작업 (Slave DB 사용)
     */
    @Transactional(readOnly = true)
    public BidStatistics getBidStatistics(Long auctionId) {
        log.info("입찰 통계 조회: 경매ID={}", auctionId);

        long bidCount = bidRepository.countByAuctionId(auctionId);
        long uniqueBidders = bidRepository.countDistinctBiddersByAuctionId(auctionId);

        return new BidStatistics(bidCount, uniqueBidders);
    }

    private void validateBid(Auction auction, User bidder, BigDecimal bidAmount) {
        // 경매 상태 확인
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new AuctionNotActiveException(auction.getId());
        }

        // 자신의 경매에 입찰 불가
        if (auction.getSeller().getId().equals(bidder.getId())) {
            throw new SellerCannotBidException();
        }

        // 최소 입찰 금액 확인
        BigDecimal minimumBid = auction.getCurrentPrice().add(auction.getMinBidIncrement());
        if (bidAmount.compareTo(minimumBid) < 0) {
            throw new BidTooLowException(minimumBid);
        }

        // 현재 최고가 입찰자인지 확인 (동일 사용자 연속 입찰 방지)
        bidRepository.findFirstByAuctionIdAndIsWinningTrue(auction.getId())
                .ifPresent(currentWinning -> {
                    if (currentWinning.getBidder().getId().equals(bidder.getId())) {
                        throw new AlreadyWinningBidderException();
                    }
                });
    }

    /**
     * 입찰 통계 내부 클래스
     */
    public record BidStatistics(long bidCount, long uniqueBidders) {}
}

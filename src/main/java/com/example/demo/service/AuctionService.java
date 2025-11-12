package com.example.demo.service;

import com.example.demo.dto.AuctionCreateRequest;
import com.example.demo.entity.Auction;
import com.example.demo.entity.AuctionStatus;
import com.example.demo.entity.User;
import com.example.demo.exception.AuctionNotFoundException;
import com.example.demo.exception.InvalidAuctionTimeException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.repository.AuctionRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;

    /**
     * 경매 생성 - 쓰기 작업 (Master DB 사용)
     */
    @Transactional
    public Auction createAuction(AuctionCreateRequest request) {
        log.info("경매 생성 중: 제목={}", request.getTitle());

        // 판매자 조회
        User seller = userRepository.findById(request.getSellerId())
                .orElseThrow(() -> new UserNotFoundException(request.getSellerId()));

        // 시간 검증
        validateAuctionTime(request.getStartTime(), request.getEndTime());

        // 최소 입찰 증가액 계산 (시작가의 1%)
        BigDecimal minIncrement = request.getStartingPrice()
                .multiply(BigDecimal.valueOf(0.01));

        Auction auction = Auction.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .startingPrice(request.getStartingPrice())
                .currentPrice(request.getStartingPrice())
                .minBidIncrement(minIncrement)
                .seller(seller)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(AuctionStatus.PENDING)
                .bidCount(0)
                .build();

        Auction savedAuction = auctionRepository.save(auction);
        log.info("경매 생성 완료: id={}", savedAuction.getId());
        return savedAuction;
    }

    /**
     * 경매 목록 조회 (페이징) - 읽기 작업 (Slave DB 사용)
     */
    @Transactional(readOnly = true)
    public Page<Auction> getAuctions(AuctionStatus status, Pageable pageable) {
        log.info("경매 목록 조회 중: 상태={}", status);

        if (status == null) {
            return auctionRepository.findAll(pageable);
        }
        return auctionRepository.findByStatus(status, pageable);
    }

    /**
     * 경매 상세 조회 - 읽기 작업 (Slave DB 사용)
     */
    @Transactional(readOnly = true)
    public Auction getAuctionById(Long id) {
        log.info("경매 상세 조회 중: id={}", id);
        return auctionRepository.findById(id)
                .orElseThrow(() -> new AuctionNotFoundException(id));
    }

    /**
     * 판매자별 경매 목록 조회 - 읽기 작업 (Slave DB 사용)
     */
    @Transactional(readOnly = true)
    public Page<Auction> getAuctionsBySeller(Long sellerId, Pageable pageable) {
        log.info("판매자별 경매 조회 중: 판매자ID={}", sellerId);
        return auctionRepository.findBySellerId(sellerId, pageable);
    }

    /**
     * 경매 상태 자동 업데이트 - 쓰기 작업 (Master DB 사용)
     * 스케줄러에서 주기적으로 호출
     */
    @Transactional
    public void updateAuctionStatuses() {
        LocalDateTime now = LocalDateTime.now();
        log.info("경매 상태 업데이트 시작: 시각={}", now);

        // PENDING → ACTIVE (시작 시간이 지난 경매)
        List<Auction> toActivate = auctionRepository
                .findByStatusAndStartTimeBefore(AuctionStatus.PENDING, now);

        toActivate.forEach(auction -> {
            auction.setStatus(AuctionStatus.ACTIVE);
            log.info("경매 활성화: id={}", auction.getId());
        });

        // ACTIVE → ENDED (종료 시간이 지난 경매)
        List<Auction> toEnd = auctionRepository
                .findByStatusAndEndTimeBefore(AuctionStatus.ACTIVE, now);

        toEnd.forEach(auction -> {
            auction.setStatus(AuctionStatus.ENDED);
            log.info("경매 종료: id={}", auction.getId());
        });

        log.info("경매 상태 업데이트 완료: ACTIVE={}건, ENDED={}건",
                toActivate.size(), toEnd.size());
    }

    /**
     * 경매 수동 종료 - 쓰기 작업 (Master DB 사용)
     */
    @Transactional
    public Auction endAuction(Long auctionId) {
        log.info("경매 수동 종료: id={}", auctionId);

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException(auctionId));

        auction.setStatus(AuctionStatus.ENDED);
        return auctionRepository.save(auction);
    }

    private void validateAuctionTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (endTime.isBefore(startTime)) {
            throw new InvalidAuctionTimeException("종료 시간은 시작 시간보다 늦어야 합니다");
        }
        if (startTime.isBefore(LocalDateTime.now())) {
            throw new InvalidAuctionTimeException("시작 시간은 현재보다 미래여야 합니다");
        }
    }
}

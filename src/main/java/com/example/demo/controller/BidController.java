package com.example.demo.controller;

import com.example.demo.dto.BidCreateRequest;
import com.example.demo.dto.BidResponse;
import com.example.demo.entity.Bid;
import com.example.demo.service.BidService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "입찰", description = "입찰 관리 API - 쓰기 작업은 Master DB, 읽기 작업은 Slave DB 사용")
@RestController
@RequestMapping("/api/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;

    @Operation(
            summary = "입찰하기",
            description = "경매에 입찰합니다. (쓰기 작업 - Master DB)\n\n" +
                    "비즈니스 규칙:\n" +
                    "- 경매가 ACTIVE 상태여야 함\n" +
                    "- 현재 최고가 + 최소 입찰 증가액 이상이어야 함\n" +
                    "- 판매자는 자신의 경매에 입찰 불가\n" +
                    "- 현재 최고가 입찰자는 재입찰 불가"
    )
    @PostMapping
    public ResponseEntity<BidResponse> placeBid(@Valid @RequestBody BidCreateRequest request) {
        Bid bid = bidService.placeBid(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BidResponse.from(bid));
    }

    @Operation(
            summary = "경매별 입찰 내역 조회",
            description = "특정 경매의 입찰 내역을 최신순으로 조회합니다. (읽기 작업 - Slave DB)"
    )
    @GetMapping("/auctions/{auctionId}")
    public ResponseEntity<Page<BidResponse>> getBidsByAuction(
            @Parameter(description = "경매 ID", required = true) @PathVariable Long auctionId,
            @PageableDefault(size = 20, sort = "bidTime", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<Bid> bids = bidService.getBidsByAuction(auctionId, pageable);
        return ResponseEntity.ok(bids.map(BidResponse::from));
    }

    @Operation(
            summary = "경매의 최근 입찰 내역 조회",
            description = "특정 경매의 최근 입찰 10개를 조회합니다. (읽기 작업 - Slave DB)"
    )
    @GetMapping("/auctions/{auctionId}/recent")
    public ResponseEntity<List<BidResponse>> getRecentBidsByAuction(
            @Parameter(description = "경매 ID", required = true) @PathVariable Long auctionId
    ) {
        List<Bid> bids = bidService.getRecentBidsByAuction(auctionId);
        return ResponseEntity.ok(bids.stream().map(BidResponse::from).toList());
    }

    @Operation(
            summary = "사용자별 입찰 내역 조회",
            description = "특정 사용자의 입찰 내역을 최신순으로 조회합니다. (읽기 작업 - Slave DB)"
    )
    @GetMapping("/users/{userId}")
    public ResponseEntity<Page<BidResponse>> getBidsByUser(
            @Parameter(description = "사용자 ID", required = true) @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "bidTime", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<Bid> bids = bidService.getBidsByUser(userId, pageable);
        return ResponseEntity.ok(bids.map(BidResponse::from));
    }

    @Operation(
            summary = "현재 최고가 입찰 조회",
            description = "경매의 현재 최고가 입찰 정보를 조회합니다. (읽기 작업 - Slave DB)"
    )
    @GetMapping("/auctions/{auctionId}/winning")
    public ResponseEntity<BidResponse> getCurrentWinningBid(
            @Parameter(description = "경매 ID", required = true) @PathVariable Long auctionId
    ) {
        Bid bid = bidService.getCurrentWinningBid(auctionId);
        if (bid == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(BidResponse.from(bid));
    }

    @Operation(
            summary = "입찰 통계 조회",
            description = "경매의 입찰 통계(총 입찰 수, 참여자 수)를 조회합니다. (읽기 작업 - Slave DB)"
    )
    @GetMapping("/auctions/{auctionId}/statistics")
    public ResponseEntity<BidService.BidStatistics> getBidStatistics(
            @Parameter(description = "경매 ID", required = true) @PathVariable Long auctionId
    ) {
        BidService.BidStatistics statistics = bidService.getBidStatistics(auctionId);
        return ResponseEntity.ok(statistics);
    }
}

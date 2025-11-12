package com.example.demo.controller;

import com.example.demo.dto.AuctionCreateRequest;
import com.example.demo.dto.AuctionDetailResponse;
import com.example.demo.dto.AuctionResponse;
import com.example.demo.entity.Auction;
import com.example.demo.entity.AuctionStatus;
import com.example.demo.service.AuctionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "경매", description = "경매 관리 API - 쓰기 작업은 Master DB, 읽기 작업은 Slave DB 사용")
@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;

    @Operation(
            summary = "경매 등록",
            description = "새로운 경매를 등록합니다. (쓰기 작업 - Master DB)"
    )
    @PostMapping
    public ResponseEntity<AuctionResponse> createAuction(
            @Valid @RequestBody AuctionCreateRequest request) {
        Auction auction = auctionService.createAuction(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AuctionResponse.from(auction));
    }

    @Operation(
            summary = "경매 목록 조회",
            description = "경매 목록을 조회합니다. 상태 필터링과 페이징이 가능합니다. (읽기 작업 - Slave DB)"
    )
    @GetMapping
    public ResponseEntity<Page<AuctionResponse>> getAuctions(
            @Parameter(description = "경매 상태 (PENDING, ACTIVE, ENDED)")
            @RequestParam(required = false) AuctionStatus status,
            @PageableDefault(size = 20, sort = "endTime", direction = Sort.Direction.ASC)
            Pageable pageable) {
        Page<Auction> auctions = auctionService.getAuctions(status, pageable);
        return ResponseEntity.ok(auctions.map(AuctionResponse::from));
    }

    @Operation(
            summary = "경매 상세 조회",
            description = "경매 ID로 상세 정보를 조회합니다. (읽기 작업 - Slave DB)"
    )
    @GetMapping("/{id}")
    public ResponseEntity<AuctionDetailResponse> getAuction(
            @Parameter(description = "경매 ID", required = true) @PathVariable Long id
    ) {
        Auction auction = auctionService.getAuctionById(id);
        return ResponseEntity.ok(AuctionDetailResponse.from(auction));
    }

    @Operation(
            summary = "판매자별 경매 목록 조회",
            description = "특정 판매자가 등록한 경매 목록을 조회합니다. (읽기 작업 - Slave DB)"
    )
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<Page<AuctionResponse>> getAuctionsBySeller(
            @Parameter(description = "판매자 ID", required = true) @PathVariable Long sellerId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<Auction> auctions = auctionService.getAuctionsBySeller(sellerId, pageable);
        return ResponseEntity.ok(auctions.map(AuctionResponse::from));
    }

    @Operation(
            summary = "경매 수동 종료",
            description = "진행 중인 경매를 수동으로 종료합니다. (쓰기 작업 - Master DB)"
    )
    @PatchMapping("/{id}/end")
    public ResponseEntity<AuctionResponse> endAuction(
            @Parameter(description = "경매 ID", required = true) @PathVariable Long id
    ) {
        Auction auction = auctionService.endAuction(id);
        return ResponseEntity.ok(AuctionResponse.from(auction));
    }

    @Operation(
            summary = "경매 상태 일괄 업데이트",
            description = "시작/종료 시간에 따라 경매 상태를 일괄 업데이트합니다. " +
                    "PENDING → ACTIVE, ACTIVE → ENDED (쓰기 작업 - Master DB)"
    )
    @PostMapping("/update-statuses")
    public ResponseEntity<Void> updateAuctionStatuses() {
        auctionService.updateAuctionStatuses();
        return ResponseEntity.ok().build();
    }
}

package com.example.demo.controller;

import com.example.demo.dto.AuctionCreateRequest;
import com.example.demo.dto.AuctionDetailResponse;
import com.example.demo.dto.AuctionResponse;
import com.example.demo.entity.Auction;
import com.example.demo.entity.AuctionStatus;
import com.example.demo.service.AuctionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;

    @PostMapping
    public ResponseEntity<AuctionResponse> createAuction(
            @Valid @RequestBody AuctionCreateRequest request) {
        Auction auction = auctionService.createAuction(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AuctionResponse.from(auction));
    }

    @GetMapping
    public ResponseEntity<Page<AuctionResponse>> getAuctions(
            @RequestParam(required = false) AuctionStatus status,
            @PageableDefault(size = 20, sort = "endTime", direction = Sort.Direction.ASC)
            Pageable pageable) {
        Page<Auction> auctions = auctionService.getAuctions(status, pageable);
        return ResponseEntity.ok(auctions.map(AuctionResponse::from));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuctionDetailResponse> getAuction(@PathVariable Long id) {
        Auction auction = auctionService.getAuctionById(id);
        return ResponseEntity.ok(AuctionDetailResponse.from(auction));
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<Page<AuctionResponse>> getAuctionsBySeller(
            @PathVariable Long sellerId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<Auction> auctions = auctionService.getAuctionsBySeller(sellerId, pageable);
        return ResponseEntity.ok(auctions.map(AuctionResponse::from));
    }

    @PatchMapping("/{id}/end")
    public ResponseEntity<AuctionResponse> endAuction(@PathVariable Long id) {
        Auction auction = auctionService.endAuction(id);
        return ResponseEntity.ok(AuctionResponse.from(auction));
    }

    @PostMapping("/update-statuses")
    public ResponseEntity<Void> updateAuctionStatuses() {
        auctionService.updateAuctionStatuses();
        return ResponseEntity.ok().build();
    }
}

package com.example.demo.controller;

import com.example.demo.dto.BidCreateRequest;
import com.example.demo.dto.BidResponse;
import com.example.demo.entity.Bid;
import com.example.demo.service.BidService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;

    @PostMapping
    public ResponseEntity<BidResponse> placeBid(@Valid @RequestBody BidCreateRequest request) {
        Bid bid = bidService.placeBid(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BidResponse.from(bid));
    }

    @GetMapping("/auctions/{auctionId}")
    public ResponseEntity<Page<BidResponse>> getBidsByAuction(
            @PathVariable Long auctionId,
            @PageableDefault(size = 20, sort = "bidTime", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<Bid> bids = bidService.getBidsByAuction(auctionId, pageable);
        return ResponseEntity.ok(bids.map(BidResponse::from));
    }

    @GetMapping("/auctions/{auctionId}/recent")
    public ResponseEntity<List<BidResponse>> getRecentBidsByAuction(
            @PathVariable Long auctionId) {
        List<Bid> bids = bidService.getRecentBidsByAuction(auctionId);
        return ResponseEntity.ok(bids.stream().map(BidResponse::from).toList());
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<Page<BidResponse>> getBidsByUser(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "bidTime", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<Bid> bids = bidService.getBidsByUser(userId, pageable);
        return ResponseEntity.ok(bids.map(BidResponse::from));
    }

    @GetMapping("/auctions/{auctionId}/winning")
    public ResponseEntity<BidResponse> getCurrentWinningBid(@PathVariable Long auctionId) {
        Bid bid = bidService.getCurrentWinningBid(auctionId);
        if (bid == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(BidResponse.from(bid));
    }

    @GetMapping("/auctions/{auctionId}/statistics")
    public ResponseEntity<BidService.BidStatistics> getBidStatistics(
            @PathVariable Long auctionId) {
        BidService.BidStatistics statistics = bidService.getBidStatistics(auctionId);
        return ResponseEntity.ok(statistics);
    }
}

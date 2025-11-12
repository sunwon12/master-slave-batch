package com.example.demo.dto;

import com.example.demo.entity.Auction;
import com.example.demo.entity.AuctionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionDetailResponse {

    private Long id;
    private String title;
    private String description;
    private BigDecimal startingPrice;
    private BigDecimal currentPrice;
    private BigDecimal minBidIncrement;
    private AuctionStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer bidCount;
    private Long sellerId;
    private String sellerName;
    private Long winnerId;
    private String winnerName;
    private LocalDateTime createdAt;

    public static AuctionDetailResponse from(Auction auction) {
        return AuctionDetailResponse.builder()
                .id(auction.getId())
                .title(auction.getTitle())
                .description(auction.getDescription())
                .startingPrice(auction.getStartingPrice())
                .currentPrice(auction.getCurrentPrice())
                .minBidIncrement(auction.getMinBidIncrement())
                .status(auction.getStatus())
                .startTime(auction.getStartTime())
                .endTime(auction.getEndTime())
                .bidCount(auction.getBidCount())
                .sellerId(auction.getSeller().getId())
                .sellerName(auction.getSeller().getUsername())
                .winnerId(auction.getWinner() != null ? auction.getWinner().getId() : null)
                .winnerName(auction.getWinner() != null ? auction.getWinner().getUsername() : null)
                .createdAt(auction.getCreatedAt())
                .build();
    }
}

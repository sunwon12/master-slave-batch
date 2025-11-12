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
public class AuctionResponse {

    private Long id;
    private String title;
    private BigDecimal startingPrice;
    private BigDecimal currentPrice;
    private AuctionStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer bidCount;
    private Long sellerId;
    private String sellerName;

    public static AuctionResponse from(Auction auction) {
        return AuctionResponse.builder()
                .id(auction.getId())
                .title(auction.getTitle())
                .startingPrice(auction.getStartingPrice())
                .currentPrice(auction.getCurrentPrice())
                .status(auction.getStatus())
                .startTime(auction.getStartTime())
                .endTime(auction.getEndTime())
                .bidCount(auction.getBidCount())
                .sellerId(auction.getSeller().getId())
                .sellerName(auction.getSeller().getUsername())
                .build();
    }
}

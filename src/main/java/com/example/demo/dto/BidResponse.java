package com.example.demo.dto;

import com.example.demo.entity.Bid;
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
public class BidResponse {

    private Long id;
    private Long auctionId;
    private String auctionTitle;
    private Long bidderId;
    private String bidderName;
    private BigDecimal bidAmount;
    private LocalDateTime bidTime;
    private Boolean isWinning;

    public static BidResponse from(Bid bid) {
        return BidResponse.builder()
                .id(bid.getId())
                .auctionId(bid.getAuction().getId())
                .auctionTitle(bid.getAuction().getTitle())
                .bidderId(bid.getBidder().getId())
                .bidderName(bid.getBidder().getUsername())
                .bidAmount(bid.getBidAmount())
                .bidTime(bid.getBidTime())
                .isWinning(bid.getIsWinning())
                .build();
    }
}

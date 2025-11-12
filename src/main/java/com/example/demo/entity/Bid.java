package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bids", indexes = {
    @Index(name = "idx_auction", columnList = "auction_id"),
    @Index(name = "idx_bidder", columnList = "bidder_id"),
    @Index(name = "idx_bid_time", columnList = "bid_time"),
    @Index(name = "idx_auction_winning", columnList = "auction_id,is_winning")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidder_id", nullable = false)
    private User bidder;

    @Column(name = "bid_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal bidAmount;

    @CreatedDate
    @Column(name = "bid_time", nullable = false, updatable = false)
    private LocalDateTime bidTime;

    @Column(name = "is_winning", nullable = false)
    @Builder.Default
    private Boolean isWinning = false;
}

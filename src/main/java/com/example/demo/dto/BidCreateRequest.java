package com.example.demo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BidCreateRequest {

    @NotNull(message = "경매 ID는 필수입니다")
    private Long auctionId;

    @NotNull(message = "입찰자 ID는 필수입니다")
    private Long bidderId;

    @NotNull(message = "입찰 금액은 필수입니다")
    @DecimalMin(value = "0.01", message = "입찰 금액은 0보다 커야 합니다")
    private BigDecimal bidAmount;
}

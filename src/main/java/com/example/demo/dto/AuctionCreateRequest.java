package com.example.demo.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuctionCreateRequest {

    @NotBlank(message = "경매 제목은 필수입니다")
    private String title;

    @NotBlank(message = "경매 설명은 필수입니다")
    private String description;

    @NotNull(message = "시작가는 필수입니다")
    @DecimalMin(value = "0.0", inclusive = false, message = "시작가는 0보다 커야 합니다")
    private BigDecimal startingPrice;

    @NotNull(message = "판매자 ID는 필수입니다")
    private Long sellerId;

    @NotNull(message = "경매 시작 시간은 필수입니다")
    @Future(message = "경매 시작 시간은 현재보다 미래여야 합니다")
    private LocalDateTime startTime;

    @NotNull(message = "경매 종료 시간은 필수입니다")
    @Future(message = "경매 종료 시간은 현재보다 미래여야 합니다")
    private LocalDateTime endTime;
}

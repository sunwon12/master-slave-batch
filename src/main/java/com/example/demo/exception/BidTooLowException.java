package com.example.demo.exception;

import java.math.BigDecimal;

public class BidTooLowException extends RuntimeException {

    public BidTooLowException(BigDecimal minimumBid) {
        super("입찰 금액이 너무 낮습니다. 최소 입찰가: " + minimumBid + "원");
    }

    public BidTooLowException(String message) {
        super(message);
    }
}

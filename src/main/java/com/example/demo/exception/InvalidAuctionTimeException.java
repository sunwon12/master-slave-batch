package com.example.demo.exception;

public class InvalidAuctionTimeException extends RuntimeException {

    public InvalidAuctionTimeException() {
        super("잘못된 경매 시간입니다: 종료 시간은 시작 시간보다 늦어야 하고, 시작 시간은 현재보다 미래여야 합니다");
    }

    public InvalidAuctionTimeException(String message) {
        super(message);
    }
}

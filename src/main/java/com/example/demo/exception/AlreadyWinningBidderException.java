package com.example.demo.exception;

public class AlreadyWinningBidderException extends RuntimeException {

    public AlreadyWinningBidderException() {
        super("이미 최고가 입찰자입니다");
    }
}

package com.example.demo.exception;

public class AuctionNotActiveException extends RuntimeException {

    public AuctionNotActiveException() {
        super("경매가 진행 중이 아닙니다");
    }

    public AuctionNotActiveException(Long auctionId) {
        super("경매가 진행 중이 아닙니다. 경매 ID: " + auctionId);
    }
}

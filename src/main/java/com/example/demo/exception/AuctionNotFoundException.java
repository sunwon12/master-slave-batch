package com.example.demo.exception;

public class AuctionNotFoundException extends RuntimeException {

    public AuctionNotFoundException(Long id) {
        super("경매를 찾을 수 없습니다. ID: " + id);
    }
}

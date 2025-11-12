package com.example.demo.exception;

public class SellerCannotBidException extends RuntimeException {

    public SellerCannotBidException() {
        super("판매자는 자신의 경매에 입찰할 수 없습니다");
    }
}

package com.example.ch7_8_test.exception;

public class OutOfStockException extends RuntimeException{

    // 상품 주문시, 재고 수량보다, 많은 주문을 할 경우, 오류를 발생시킴.
    // 기본적인 유효성 체크.
    public OutOfStockException(String message) {
        super(message);
    }

}
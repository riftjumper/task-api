package com.example.task_api.order;

public class OrderApiException extends RuntimeException {
    public enum Reason {
        PRODUCT_NOT_FOUND,
        INSUFFICIENT_STOCK,
        ORDER_NOT_FOUND
    }

    private final Reason reason;

    public OrderApiException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}

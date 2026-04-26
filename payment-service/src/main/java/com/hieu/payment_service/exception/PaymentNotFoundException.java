package com.hieu.payment_service.exception;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(Long id) {
        super("Payment not found: " + id);
    }

    public PaymentNotFoundException(String orderId) {
        super("Payment not found for order: " + orderId);
    }
}

package com.hieu.payment_service.exception;

public class PaymentAccessDeniedException extends RuntimeException {

    public PaymentAccessDeniedException(Long paymentId) {
        super("Access denied for payment: " + paymentId);
    }
}

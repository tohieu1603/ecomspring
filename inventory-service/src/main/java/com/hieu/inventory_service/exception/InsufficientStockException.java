package com.hieu.inventory_service.exception;

/** Thrown when available stock cannot satisfy a reservation request. */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(Long productId, int available, int requested) {
        super(String.format("Insufficient stock for product %d: available=%d, requested=%d",
            productId, available, requested));
    }

    public InsufficientStockException(String message) {
        super(message);
    }
}

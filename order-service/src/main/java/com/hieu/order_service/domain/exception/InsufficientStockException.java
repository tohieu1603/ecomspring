package com.hieu.order_service.domain.exception;

import com.hieu.order_service.domain.shared.DomainException;

public final class InsufficientStockException extends DomainException {
    public InsufficientStockException(String message) {
        super(OrderErrorCodes.ORDER_INSUFFICIENT_STOCK, message);
    }
}

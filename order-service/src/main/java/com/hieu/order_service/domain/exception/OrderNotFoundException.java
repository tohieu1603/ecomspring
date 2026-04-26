package com.hieu.order_service.domain.exception;

import com.hieu.order_service.domain.shared.DomainException;

public final class OrderNotFoundException extends DomainException {
    public OrderNotFoundException(Object id) {
        super(OrderErrorCodes.ORDER_NOT_FOUND, "Order not found: " + id);
    }
}

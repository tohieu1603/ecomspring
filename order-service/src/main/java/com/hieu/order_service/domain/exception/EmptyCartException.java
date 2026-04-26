package com.hieu.order_service.domain.exception;

import com.hieu.order_service.domain.shared.DomainException;

public final class EmptyCartException extends DomainException {
    public EmptyCartException(String userId) {
        super(OrderErrorCodes.ORDER_EMPTY_CART, "Cart is empty for user: " + userId);
    }
}

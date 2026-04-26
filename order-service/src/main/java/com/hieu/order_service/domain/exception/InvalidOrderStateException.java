package com.hieu.order_service.domain.exception;

import com.hieu.order_service.domain.shared.DomainException;

public final class InvalidOrderStateException extends DomainException {
    public InvalidOrderStateException(String message) {
        super(OrderErrorCodes.ORDER_INVALID_STATE, message);
    }
}

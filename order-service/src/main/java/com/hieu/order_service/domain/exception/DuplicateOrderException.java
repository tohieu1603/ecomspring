package com.hieu.order_service.domain.exception;

import com.hieu.order_service.domain.shared.DomainException;

public final class DuplicateOrderException extends DomainException {
    public DuplicateOrderException(String key) {
        super(OrderErrorCodes.ORDER_DUPLICATE, "Duplicate order in progress for key: " + key);
    }
}

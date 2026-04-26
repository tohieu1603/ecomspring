package com.hieu.order_service.domain.exception;

import com.hieu.order_service.domain.shared.DomainException;

public final class ReturnRequestNotFoundException extends DomainException {
    public ReturnRequestNotFoundException(Object id) {
        super(OrderErrorCodes.RETURN_REQUEST_NOT_FOUND, "ReturnRequest not found: " + id);
    }
}

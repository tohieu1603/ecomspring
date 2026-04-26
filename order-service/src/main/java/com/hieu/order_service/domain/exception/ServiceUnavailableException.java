package com.hieu.order_service.domain.exception;

import com.hieu.order_service.domain.shared.DomainException;

public final class ServiceUnavailableException extends DomainException {
    public ServiceUnavailableException(String message) {
        super(OrderErrorCodes.ORDER_SERVICE_UNAVAILABLE, message);
    }
}

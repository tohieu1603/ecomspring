package com.hieu.order_service.domain.model.order.valueobject;

import java.util.Objects;

public record ReturnRequestId(Long value) {

    public ReturnRequestId { Objects.requireNonNull(value, "ReturnRequestId value"); }

    public static ReturnRequestId of(Long value) { return new ReturnRequestId(value); }
}

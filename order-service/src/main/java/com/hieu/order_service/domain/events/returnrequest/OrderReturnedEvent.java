package com.hieu.order_service.domain.events.returnrequest;

import com.hieu.order_service.domain.events.DomainEvent;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public final class OrderReturnedEvent extends DomainEvent {
    private final Long orderId;
    private final Long returnRequestId;
    private final String userId;
    private final BigDecimal refundAmount;

    public OrderReturnedEvent(Long orderId, Long returnRequestId, String userId, BigDecimal refundAmount) {
        this.orderId = orderId;
        this.returnRequestId = returnRequestId;
        this.userId = userId;
        this.refundAmount = refundAmount;
    }

    @Override public String aggregateId() { return String.valueOf(orderId); }
}

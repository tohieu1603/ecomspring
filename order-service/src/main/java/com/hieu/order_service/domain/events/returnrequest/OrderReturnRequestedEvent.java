package com.hieu.order_service.domain.events.returnrequest;

import com.hieu.order_service.domain.events.DomainEvent;
import lombok.Getter;

@Getter
public final class OrderReturnRequestedEvent extends DomainEvent {
    private final Long orderId;
    private final Long returnRequestId;
    private final String userId;
    private final String reason;

    public OrderReturnRequestedEvent(Long orderId, Long returnRequestId, String userId, String reason) {
        this.orderId = orderId;
        this.returnRequestId = returnRequestId;
        this.userId = userId;
        this.reason = reason;
    }

    @Override public String aggregateId() { return String.valueOf(orderId); }
}

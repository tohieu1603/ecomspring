package com.hieu.order_service.domain.events.returnrequest;

import com.hieu.order_service.domain.events.DomainEvent;
import lombok.Getter;

@Getter
public final class OrderReturnRejectedEvent extends DomainEvent {
    private final Long orderId;
    private final Long returnRequestId;
    private final String userId;

    public OrderReturnRejectedEvent(Long orderId, Long returnRequestId, String userId) {
        this.orderId = orderId;
        this.returnRequestId = returnRequestId;
        this.userId = userId;
    }

    @Override public String aggregateId() { return String.valueOf(orderId); }
}

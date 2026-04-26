package com.hieu.order_service.domain.events.order;

import com.hieu.order_service.domain.events.DomainEvent;
import lombok.Getter;

@Getter
public final class OrderFailedEvent extends DomainEvent {
    private final Long orderId;
    private final String orderNumber;
    private final String userId;
    private final String reason;

    public OrderFailedEvent(Long orderId, String orderNumber, String userId, String reason) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.reason = reason;
    }

    @Override public String aggregateId() { return String.valueOf(orderId); }
}

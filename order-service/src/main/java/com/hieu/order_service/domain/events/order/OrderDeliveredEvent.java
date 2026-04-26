package com.hieu.order_service.domain.events.order;

import com.hieu.order_service.domain.events.DomainEvent;
import lombok.Getter;

@Getter
public final class OrderDeliveredEvent extends DomainEvent {
    private final Long orderId;
    private final String orderNumber;
    private final String userId;

    public OrderDeliveredEvent(Long orderId, String orderNumber, String userId) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.userId = userId;
    }

    @Override public String aggregateId() { return String.valueOf(orderId); }
}

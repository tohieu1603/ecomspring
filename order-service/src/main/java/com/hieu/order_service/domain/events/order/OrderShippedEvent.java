package com.hieu.order_service.domain.events.order;

import com.hieu.order_service.domain.events.DomainEvent;
import lombok.Getter;

@Getter
public final class OrderShippedEvent extends DomainEvent {
    private final Long orderId;
    private final String orderNumber;
    private final String userId;
    private final Long shipmentId;

    public OrderShippedEvent(Long orderId, String orderNumber, String userId, Long shipmentId) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.shipmentId = shipmentId;
    }

    @Override public String aggregateId() { return String.valueOf(orderId); }
}

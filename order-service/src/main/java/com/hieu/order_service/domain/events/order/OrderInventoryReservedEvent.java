package com.hieu.order_service.domain.events.order;

import com.hieu.order_service.domain.events.DomainEvent;
import lombok.Getter;

@Getter
public final class OrderInventoryReservedEvent extends DomainEvent {
    private final Long orderId;
    private final String orderNumber;
    private final String reservationId;

    public OrderInventoryReservedEvent(Long orderId, String orderNumber, String reservationId) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.reservationId = reservationId;
    }

    @Override public String aggregateId() { return String.valueOf(orderId); }
}

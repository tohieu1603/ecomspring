package com.hieu.order_service.domain.events.order;

import com.hieu.order_service.domain.events.DomainEvent;
import lombok.Getter;

@Getter
public final class OrderConfirmedEvent extends DomainEvent {
    private final Long orderId;
    private final String orderNumber;
    private final String userId;
    private final Long paymentId;

    public OrderConfirmedEvent(Long orderId, String orderNumber, String userId, Long paymentId) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.paymentId = paymentId;
    }

    @Override public String aggregateId() { return String.valueOf(orderId); }
}

package com.hieu.order_service.domain.events.order;

import com.hieu.order_service.domain.events.DomainEvent;
import lombok.Getter;

@Getter
public final class OrderPaymentInitiatedEvent extends DomainEvent {
    private final Long orderId;
    private final String orderNumber;
    private final Long paymentId;

    public OrderPaymentInitiatedEvent(Long orderId, String orderNumber, Long paymentId) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.paymentId = paymentId;
    }

    @Override public String aggregateId() { return String.valueOf(orderId); }
}

package com.hieu.order_service.domain.events.order;

import com.hieu.order_service.domain.events.DomainEvent;
import lombok.Getter;

@Getter
public final class OrderCancelledEvent extends DomainEvent {
    private final Long orderId;
    private final String orderNumber;
    private final String userId;
    private final String reason;
    private final String voucherCode;

    public OrderCancelledEvent(Long orderId, String orderNumber, String userId, String reason, String voucherCode) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.reason = reason;
        this.voucherCode = voucherCode;
    }

    @Override public String aggregateId() { return String.valueOf(orderId); }
}

package com.hieu.order_service.domain.events.returnrequest;

import com.hieu.order_service.domain.events.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderReturnedEvent(
        UUID eventId,
        Instant occurredOn,
        Long orderId,
        Long returnRequestId,
        String userId,
        BigDecimal refundAmount
) implements DomainEvent {

    public OrderReturnedEvent(Long orderId, Long returnRequestId, String userId, BigDecimal refundAmount) {
        this(UUID.randomUUID(), Instant.now(), orderId, returnRequestId, userId, refundAmount);
    }

    @Override
    public String aggregateId() { return String.valueOf(orderId); }
}

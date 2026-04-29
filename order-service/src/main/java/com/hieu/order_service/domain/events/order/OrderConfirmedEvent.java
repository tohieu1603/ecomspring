package com.hieu.order_service.domain.events.order;

import com.hieu.order_service.domain.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record OrderConfirmedEvent(
        UUID eventId,
        Instant occurredOn,
        Long orderId,
        String orderNumber,
        String userId,
        Long paymentId
) implements DomainEvent {

    /** Convenience factory — generates eventId and occurredOn automatically. */
    public OrderConfirmedEvent(Long orderId, String orderNumber, String userId, Long paymentId) {
        this(UUID.randomUUID(), Instant.now(), orderId, orderNumber, userId, paymentId);
    }

    @Override
    public String aggregateId() { return String.valueOf(orderId); }
}

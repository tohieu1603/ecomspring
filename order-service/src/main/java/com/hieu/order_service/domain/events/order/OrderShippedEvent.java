package com.hieu.order_service.domain.events.order;

import com.hieu.order_service.domain.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record OrderShippedEvent(
        UUID eventId,
        Instant occurredOn,
        Long orderId,
        String orderNumber,
        String userId,
        Long shipmentId
) implements DomainEvent {

    /** Convenience factory — generates eventId and occurredOn automatically. */
    public OrderShippedEvent(Long orderId, String orderNumber, String userId, Long shipmentId) {
        this(UUID.randomUUID(), Instant.now(), orderId, orderNumber, userId, shipmentId);
    }

    @Override
    public String aggregateId() { return String.valueOf(orderId); }
}

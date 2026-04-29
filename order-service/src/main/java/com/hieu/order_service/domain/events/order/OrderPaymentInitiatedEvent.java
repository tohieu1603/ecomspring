package com.hieu.order_service.domain.events.order;

import com.hieu.order_service.domain.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record OrderPaymentInitiatedEvent(
        UUID eventId,
        Instant occurredOn,
        Long orderId,
        String orderNumber,
        Long paymentId
) implements DomainEvent {

    /** Convenience factory — generates eventId and occurredOn automatically. */
    public OrderPaymentInitiatedEvent(Long orderId, String orderNumber, Long paymentId) {
        this(UUID.randomUUID(), Instant.now(), orderId, orderNumber, paymentId);
    }

    @Override
    public String aggregateId() { return String.valueOf(orderId); }
}

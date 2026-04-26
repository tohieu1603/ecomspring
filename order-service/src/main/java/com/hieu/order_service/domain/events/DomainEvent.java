package com.hieu.order_service.domain.events;

import java.time.Instant;
import java.util.UUID;

/** Base class for in-process domain events raised by order aggregates. */
public abstract class DomainEvent {

    private final UUID eventId = UUID.randomUUID();
    private final Instant occurredOn = Instant.now();

    public final UUID eventId()       { return eventId; }
    public final Instant occurredOn() { return occurredOn; }

    public String eventType() { return getClass().getSimpleName(); }

    /** Aggregate id — used as Kafka partition key. */
    public abstract String aggregateId();

    @Override
    public String toString() {
        return "%s{eventId=%s, aggregateId=%s, occurredOn=%s}"
                .formatted(eventType(), eventId, aggregateId(), occurredOn);
    }
}

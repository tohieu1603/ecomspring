package com.hieu.shipping_service.kafka;

import java.time.Instant;
import java.util.UUID;

/**
 * Integration event emitted on every shipment status transition.
 * Published to {@code shipping.status-changed}.
 */
public record ShipmentStatusChangedEvent(
        String eventId,
        Instant occurredOn,
        Long shipmentId,
        String orderId,
        String userId,
        String oldStatus,
        String newStatus,
        String trackingNumber
) {
    public static ShipmentStatusChangedEvent of(Long shipmentId, String orderId, String userId,
                                                 String oldStatus, String newStatus,
                                                 String trackingNumber) {
        return new ShipmentStatusChangedEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                shipmentId, orderId, userId,
                oldStatus, newStatus, trackingNumber);
    }
}

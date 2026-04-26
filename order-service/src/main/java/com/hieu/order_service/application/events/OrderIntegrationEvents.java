package com.hieu.order_service.application.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** JSON-serialisable integration events for order family. Public contract with downstream services. */
public final class OrderIntegrationEvents {

    private OrderIntegrationEvents() {}

    public record ItemSnapshot(Long productId, String productName, int quantity, BigDecimal unitPrice) {}

    public record OrderPlaced(
            UUID eventId, Instant occurredOn,
            Long orderId, String orderNumber, String userId, BigDecimal totalAmount,
            List<ItemSnapshot> items,
            String street, String ward, String district, String city, String country
    ) {}

    public record OrderConfirmed(
            UUID eventId, Instant occurredOn,
            Long orderId, String orderNumber, String userId, Long paymentId
    ) {}

    public record OrderShipped(
            UUID eventId, Instant occurredOn,
            Long orderId, String orderNumber, String userId, Long shipmentId
    ) {}

    public record OrderDelivered(
            UUID eventId, Instant occurredOn,
            Long orderId, String orderNumber, String userId
    ) {}

    public record OrderCancelled(
            UUID eventId, Instant occurredOn,
            Long orderId, String orderNumber, String userId, String reason, String voucherCode
    ) {}

    public record OrderFailed(
            UUID eventId, Instant occurredOn,
            Long orderId, String orderNumber, String userId, String reason
    ) {}

    public record OrderReturnRequested(
            UUID eventId, Instant occurredOn,
            Long orderId, Long returnRequestId, String userId, String reason
    ) {}

    public record OrderReturnApproved(
            UUID eventId, Instant occurredOn,
            Long orderId, Long returnRequestId, String userId
    ) {}

    public record OrderReturnRejected(
            UUID eventId, Instant occurredOn,
            Long orderId, Long returnRequestId, String userId
    ) {}

    public record OrderReturned(
            UUID eventId, Instant occurredOn,
            Long orderId, Long returnRequestId, String userId, BigDecimal refundAmount
    ) {}
}

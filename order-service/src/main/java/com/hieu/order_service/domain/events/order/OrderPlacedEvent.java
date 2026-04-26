package com.hieu.order_service.domain.events.order;

import com.hieu.order_service.domain.events.DomainEvent;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Fired after an order is persisted with initial state. Consumers (analytics, search
 * signals, recommendation, shipping) need the full snapshot — items, total, and the
 * shipping address — so we carry them on the event instead of forcing each consumer
 * to round-trip back to order-service.
 */
@Getter
public final class OrderPlacedEvent extends DomainEvent {

    private final Long orderId;
    private final String orderNumber;
    private final String userId;
    private final BigDecimal totalAmount;
    private final String paymentMethod;
    private final AddressSnapshot shippingAddress;
    private final List<ItemSnapshot> items;

    public OrderPlacedEvent(Long orderId, String orderNumber, String userId,
                             BigDecimal totalAmount, String paymentMethod,
                             AddressSnapshot shippingAddress, List<ItemSnapshot> items) {
        this.orderId = Objects.requireNonNull(orderId, "orderId");
        this.orderNumber = Objects.requireNonNull(orderNumber, "orderNumber");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.totalAmount = Objects.requireNonNull(totalAmount, "totalAmount");
        this.paymentMethod = paymentMethod;
        this.shippingAddress = shippingAddress;
        this.items = List.copyOf(Objects.requireNonNullElse(items, List.of()));
    }

    @Override public String aggregateId() { return String.valueOf(orderId); }

    /** Flat projection of the shipping address — avoids leaking the VO across services. */
    public record AddressSnapshot(String recipientName, String recipientPhone,
                                   String street, String ward, String district,
                                   String city, String country, String postalCode) {}

    /** Flat projection of an order item. */
    public record ItemSnapshot(Long productId, String productName, Long variantId,
                                String variantSku, BigDecimal unitPrice, int quantity) {}
}

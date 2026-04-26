package com.hieu.order_service.infrastructure.messaging;

import com.hieu.order_service.application.events.KafkaTopics;
import com.hieu.order_service.application.events.OrderIntegrationEvents;
import com.hieu.order_service.domain.events.DomainEvent;
import com.hieu.order_service.domain.events.order.*;
import com.hieu.order_service.domain.events.returnrequest.*;
import org.springframework.stereotype.Component;

/** Maps in-process domain events to Kafka-bound integration event records. */
@Component
public class DomainEventToIntegrationMapper {

    public record Routed(String topic, String key, Object payload) {}

    public Routed map(DomainEvent event) {
        return switch (event) {
            case OrderPlacedEvent e -> {
                var addr = e.getShippingAddress();
                var items = e.getItems().stream()
                        .map(i -> new OrderIntegrationEvents.ItemSnapshot(
                                i.productId(), i.productName(), i.quantity(), i.unitPrice()))
                        .toList();
                yield new Routed(KafkaTopics.ORDER_PLACED, e.aggregateId(),
                        new OrderIntegrationEvents.OrderPlaced(
                                e.eventId(), e.occurredOn(),
                                e.getOrderId(), e.getOrderNumber(), e.getUserId(),
                                e.getTotalAmount(), items,
                                addr != null ? addr.street() : null,
                                addr != null ? addr.ward() : null,
                                addr != null ? addr.district() : null,
                                addr != null ? addr.city() : null,
                                addr != null ? addr.country() : null));
            }

            case OrderConfirmedEvent e -> new Routed(KafkaTopics.ORDER_CONFIRMED, e.aggregateId(),
                    new OrderIntegrationEvents.OrderConfirmed(e.eventId(), e.occurredOn(),
                            e.getOrderId(), e.getOrderNumber(), e.getUserId(), e.getPaymentId()));

            case OrderShippedEvent e -> new Routed(KafkaTopics.ORDER_SHIPPED, e.aggregateId(),
                    new OrderIntegrationEvents.OrderShipped(e.eventId(), e.occurredOn(),
                            e.getOrderId(), e.getOrderNumber(), e.getUserId(), e.getShipmentId()));

            case OrderDeliveredEvent e -> new Routed(KafkaTopics.ORDER_DELIVERED, e.aggregateId(),
                    new OrderIntegrationEvents.OrderDelivered(e.eventId(), e.occurredOn(),
                            e.getOrderId(), e.getOrderNumber(), e.getUserId()));

            case OrderCancelledEvent e -> new Routed(KafkaTopics.ORDER_CANCELLED, e.aggregateId(),
                    new OrderIntegrationEvents.OrderCancelled(e.eventId(), e.occurredOn(),
                            e.getOrderId(), e.getOrderNumber(), e.getUserId(), e.getReason(), e.getVoucherCode()));

            case OrderFailedEvent e -> new Routed(KafkaTopics.ORDER_FAILED, e.aggregateId(),
                    new OrderIntegrationEvents.OrderFailed(e.eventId(), e.occurredOn(),
                            e.getOrderId(), e.getOrderNumber(), e.getUserId(), e.getReason()));

            case OrderReturnRequestedEvent e -> new Routed(KafkaTopics.ORDER_RETURN_REQUESTED, e.aggregateId(),
                    new OrderIntegrationEvents.OrderReturnRequested(e.eventId(), e.occurredOn(),
                            e.getOrderId(), e.getReturnRequestId(), e.getUserId(), e.getReason()));

            case OrderReturnApprovedEvent e -> new Routed(KafkaTopics.ORDER_RETURN_APPROVED, e.aggregateId(),
                    new OrderIntegrationEvents.OrderReturnApproved(e.eventId(), e.occurredOn(),
                            e.getOrderId(), e.getReturnRequestId(), e.getUserId()));

            case OrderReturnRejectedEvent e -> new Routed(KafkaTopics.ORDER_RETURN_REJECTED, e.aggregateId(),
                    new OrderIntegrationEvents.OrderReturnRejected(e.eventId(), e.occurredOn(),
                            e.getOrderId(), e.getReturnRequestId(), e.getUserId()));

            case OrderReturnedEvent e -> new Routed(KafkaTopics.ORDER_RETURNED, e.aggregateId(),
                    new OrderIntegrationEvents.OrderReturned(e.eventId(), e.occurredOn(),
                            e.getOrderId(), e.getReturnRequestId(), e.getUserId(), e.getRefundAmount()));

            default -> null;
        };
    }
}

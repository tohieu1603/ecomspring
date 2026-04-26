package com.hieu.shipping_service.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes shipping integration events after the DB transaction commits.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShipmentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatusChanged(ShipmentStatusChangedEvent event) {
        kafkaTemplate.send(ShippingTopics.SHIPPING_STATUS_CHANGED, event.orderId(), event);
        log.info("Published shipping.status-changed: shipment={} {} -> {}",
                event.shipmentId(), event.oldStatus(), event.newStatus());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDelivered(ShipmentDeliveredEvent event) {
        kafkaTemplate.send(ShippingTopics.SHIPPING_DELIVERED, event.orderId(), event);
        log.info("Published shipping.delivered: shipment={} order={}", event.shipmentId(), event.orderId());
    }
}

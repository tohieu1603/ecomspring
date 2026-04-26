package com.hieu.order_service.infrastructure.messaging;

import com.hieu.order_service.application.common.DomainEventPublisher;
import com.hieu.order_service.domain.exception.OrderNotFoundException;
import com.hieu.order_service.domain.model.order.valueobject.OrderNumber;
import com.hieu.order_service.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/** Consumes payment events from payment-service and transitions order state. */
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final OrderRepository orderRepository;
    private final DomainEventPublisher eventPublisher;

    @KafkaListener(topics = {"payment.completed", "payment.failed"}, groupId = "order-service")
    @Transactional
    public void onPaymentEvent(Map<String, Object> payload,
                               org.apache.kafka.clients.consumer.ConsumerRecord<String, Object> record) {
        try {
            var topic = record.topic();
            var orderNumber = payload.get("orderId") != null ? payload.get("orderId").toString() : null;
            if (orderNumber == null) { log.warn("Payment event missing orderId"); return; }

            var order = orderRepository.findByOrderNumber(OrderNumber.of(orderNumber))
                    .orElseThrow(() -> new OrderNotFoundException(orderNumber));

            if ("payment.completed".equals(topic)) {
                order.markPaymentCompleted();
                order.confirm();
            } else {
                var reason = payload.get("reason") != null ? payload.get("reason").toString() : "Payment failed";
                order.markFailed(reason);
            }

            var saved = orderRepository.save(order);
            eventPublisher.publishEventsOf(saved);
        } catch (Exception e) {
            log.error("Failed to process payment event: {}", e.getMessage(), e);
        }
    }
}

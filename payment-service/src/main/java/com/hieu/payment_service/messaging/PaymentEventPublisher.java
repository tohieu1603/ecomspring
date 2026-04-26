package com.hieu.payment_service.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges in-process spring events to Kafka AFTER_COMMIT.
 * Failures are logged but not retried (upgrade to Transactional Outbox for at-least-once).
 */
@Component
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCompleted(PaymentIntegrationEvents.PaymentCompletedEvent event) {
        send(KafkaTopics.PAYMENT_COMPLETED, event.paymentId().toString(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentRefunded(PaymentIntegrationEvents.PaymentRefundedEvent event) {
        send(KafkaTopics.PAYMENT_REFUNDED, event.paymentId().toString(), event);
    }

    private void send(String topic, String key, Object payload) {
        try {
            kafkaTemplate.send(topic, key, payload);
            log.debug("Published to {} key={}", topic, key);
        } catch (Exception e) {
            log.warn("Failed to publish to {} key={}: {}", topic, key, e.getMessage());
        }
    }
}

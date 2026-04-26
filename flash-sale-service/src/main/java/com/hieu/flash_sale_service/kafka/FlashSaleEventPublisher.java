package com.hieu.flash_sale_service.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Listens to in-process domain events and publishes them to Kafka after TX commit. */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlashSaleEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStarted(FlashSaleStartedEvent event) {
        kafkaTemplate.send(KafkaTopics.FLASH_SALE_STARTED, String.valueOf(event.saleId()), event);
        log.info("Published flashsale.started saleId={}", event.saleId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEnded(FlashSaleEndedEvent event) {
        kafkaTemplate.send(KafkaTopics.FLASH_SALE_ENDED, String.valueOf(event.saleId()), event);
        log.info("Published flashsale.ended saleId={}", event.saleId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSlotReserved(FlashSaleSlotReservedEvent event) {
        kafkaTemplate.send(KafkaTopics.FLASH_SALE_SLOT_RESERVED, String.valueOf(event.saleId()), event);
        log.debug("Published flashsale.slot-reserved saleId={} userId={}", event.saleId(), event.userId());
    }
}

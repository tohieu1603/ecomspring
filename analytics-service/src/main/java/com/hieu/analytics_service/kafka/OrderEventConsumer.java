package com.hieu.analytics_service.kafka;

import com.hieu.analytics_service.service.AnalyticsIndexer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final AnalyticsIndexer indexer;

    @KafkaListener(topics = {
            "order.placed", "order.confirmed", "order.cancelled",
            "order.shipped", "order.delivered", "order.failed"
    }, groupId = "${spring.kafka.consumer.group-id}")
    public void onOrderEvent(Map<String, Object> payload,
                             @Header(name = "kafka_receivedTopic", required = false) String topic) {
        try {
            String fallbackType = topic != null ? topic.toUpperCase().replace('.', '_') : "ORDER_EVENT";
            indexer.index(EventPayloadMapper.toDocument(payload, fallbackType, "ORDER"));
        } catch (Exception e) {
            log.error("Failed indexing order event: {}", e.getMessage(), e);
        }
    }
}

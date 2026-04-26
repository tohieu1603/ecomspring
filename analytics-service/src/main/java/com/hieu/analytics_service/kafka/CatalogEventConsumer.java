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
public class CatalogEventConsumer {

    private final AnalyticsIndexer indexer;

    @KafkaListener(topics = {
            "catalog.product-created", "catalog.product-updated",
            "catalog.product-status-changed", "catalog.product-deleted",
            "catalog.variant-stock-changed", "catalog.variant-price-changed"
    }, groupId = "${spring.kafka.consumer.group-id}")
    public void onCatalogEvent(Map<String, Object> payload,
                               @Header(name = "kafka_receivedTopic", required = false) String topic) {
        try {
            String fallbackType = topic != null ? topic.toUpperCase().replace('.', '_') : "CATALOG_EVENT";
            indexer.index(EventPayloadMapper.toDocument(payload, fallbackType, "PRODUCT"));
        } catch (Exception e) {
            log.error("Failed indexing catalog event: {}", e.getMessage(), e);
        }
    }
}

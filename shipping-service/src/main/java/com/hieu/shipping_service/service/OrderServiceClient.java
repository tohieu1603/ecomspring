package com.hieu.shipping_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * HTTP client for order-service.
 * Used by the Kafka consumer to fetch shipping address after payment.completed.
 */
@Slf4j
@Component
public class OrderServiceClient {

    private final WebClient webClient;

    public OrderServiceClient(@Value("${order-service.base-url:http://localhost:8085}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    /**
     * Fetches the order response map. Returns empty if order-service is unreachable.
     */
    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> fetchOrder(String orderId) {
        try {
            var result = webClient.get()
                    .uri("/api/orders/{id}", orderId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .onErrorResume(e -> {
                        log.warn("order-service unreachable for orderId={}: {}", orderId, e.getMessage());
                        return Mono.empty();
                    })
                    .block();
            return Optional.ofNullable((Map<String, Object>) result);
        } catch (Exception e) {
            log.warn("Failed to fetch order {}: {}", orderId, e.getMessage());
            return Optional.empty();
        }
    }
}

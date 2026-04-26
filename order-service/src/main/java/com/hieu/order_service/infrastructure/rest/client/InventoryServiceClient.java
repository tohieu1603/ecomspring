package com.hieu.order_service.infrastructure.rest.client;

import com.hieu.order_service.domain.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST companion to {@link com.hieu.order_service.infrastructure.grpc.client.InventoryGrpcClient}.
 * Uses inventory-service's internal endpoints ({@code /api/inventory/reserve|confirm|release})
 * which are not JWT-protected (service-to-service trust zone).
 */
@Component
@Slf4j
public class InventoryServiceClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestTemplate restTemplate;
    private final String inventoryServiceUrl;

    public InventoryServiceClient(RestTemplate restTemplate,
                                   @Value("${services.inventory-url:http://localhost:8088}") String inventoryServiceUrl) {
        this.restTemplate = restTemplate;
        this.inventoryServiceUrl = inventoryServiceUrl;
    }

    /** Reserve stock. Throws {@link ServiceUnavailableException} on transport failure. */
    public ReservationResult reserveStock(String orderId, List<ReserveItem> items) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderId", orderId);
        body.put("items", items);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    inventoryServiceUrl + "/api/inventory/reserve",
                    HttpMethod.POST, entity, MAP_TYPE);
            Map<String, Object> payload = unwrap(resp.getBody());
            if (payload == null) throw new ServiceUnavailableException("inventory-service");
            return new ReservationResult(
                    Boolean.TRUE.equals(payload.get("success")),
                    (String) payload.get("reservationId"),
                    (String) payload.get("errorMessage"));
        } catch (RestClientException e) {
            log.error("REST inventory.reserveStock({}) failed: {}", orderId, e.getMessage());
            throw new ServiceUnavailableException("inventory-service");
        }
    }

    public boolean confirmReservation(String orderId) {
        return postWithQuery("/api/inventory/confirm", orderId);
    }

    public boolean releaseReservation(String orderId) {
        return postWithQuery("/api/inventory/release", orderId);
    }

    private boolean postWithQuery(String path, String orderId) {
        String url = UriComponentsBuilder.fromUriString(inventoryServiceUrl + path)
                .queryParam("orderId", orderId).toUriString();
        try {
            restTemplate.exchange(url, HttpMethod.POST, HttpEntity.EMPTY, MAP_TYPE);
            return true;
        } catch (RestClientException e) {
            log.warn("REST inventory {} failed for {}: {}", path, orderId, e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> unwrap(Map<String, Object> body) {
        if (body == null) return null;
        Object data = body.get("data");
        if (data instanceof Map<?, ?> inner) return (Map<String, Object>) inner;
        return body;
    }

    public record ReserveItem(Long productId, int quantity) {}
    public record ReservationResult(boolean success, String reservationId, String errorMessage) {}
}

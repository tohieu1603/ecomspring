package com.hieu.cart_service.rest.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * REST companion to the catalog gRPC client. Cart uses it to revalidate line items when
 * gRPC is unavailable — failures degrade to {@link Optional#empty()} so the cart still
 * renders with stale prices + a warning.
 */
@Component
@Slf4j
public class CatalogServiceClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestTemplate restTemplate;
    private final String catalogServiceUrl;

    public CatalogServiceClient(RestTemplate restTemplate,
                                 @Value("${services.catalog-url:http://localhost:8083}") String catalogServiceUrl) {
        this.restTemplate = restTemplate;
        this.catalogServiceUrl = catalogServiceUrl;
    }

    public Optional<VariantSnapshot> getVariantBySku(String sku) {
        try {
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    catalogServiceUrl + "/api/variants/by-sku/" + sku,
                    HttpMethod.GET, HttpEntity.EMPTY, MAP_TYPE);
            Map<String, Object> data = unwrap(resp.getBody());
            if (data == null) return Optional.empty();
            return Optional.of(toSnapshot(data));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            log.debug("REST catalog.getVariantBySku({}) failed: {}", sku, e.getMessage());
            return Optional.empty();
        }
    }

    private static VariantSnapshot toSnapshot(Map<String, Object> d) {
        return new VariantSnapshot(
                ((Number) d.get("id")).longValue(),
                d.get("productId") != null ? ((Number) d.get("productId")).longValue() : null,
                (String) d.get("sku"),
                d.get("price") != null ? new BigDecimal(d.get("price").toString()) : BigDecimal.ZERO,
                (String) d.get("image"),
                d.get("quantity") != null ? ((Number) d.get("quantity")).intValue() : 0,
                (String) d.get("status"),
                Boolean.TRUE.equals(d.get("available")));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> unwrap(Map<String, Object> body) {
        if (body == null) return null;
        Object data = body.get("data");
        if (data instanceof Map<?, ?> inner) return (Map<String, Object>) inner;
        return body;
    }

    public record VariantSnapshot(Long id, Long productId, String sku, BigDecimal price,
                                   String image, int quantity, String status, boolean available) {}
}

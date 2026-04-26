package com.hieu.order_service.infrastructure.rest.client;

import com.hieu.order_service.domain.exception.ServiceUnavailableException;
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
 * REST companion to {@link com.hieu.order_service.infrastructure.grpc.client.CatalogGrpcClient}.
 * Public catalog endpoints (GET product / variant) don't require JWT — no auth header needed.
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

    public Optional<VariantResponse> getVariantBySku(String sku) {
        try {
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    catalogServiceUrl + "/api/variants/by-sku/" + sku,
                    HttpMethod.GET, HttpEntity.EMPTY, MAP_TYPE);
            Map<String, Object> data = unwrap(resp.getBody());
            if (data == null) return Optional.empty();
            Long id = data.get("id") != null ? ((Number) data.get("id")).longValue() : null;
            Long productId = data.get("productId") != null ? ((Number) data.get("productId")).longValue() : null;
            BigDecimal price = data.get("price") != null ? new BigDecimal(data.get("price").toString()) : BigDecimal.ZERO;
            int quantity = data.get("quantity") != null ? ((Number) data.get("quantity")).intValue() : 0;
            return Optional.of(new VariantResponse(id, productId, sku, price, quantity,
                    (String) data.get("status")));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            log.error("REST catalog.getVariantBySku({}) failed: {}", sku, e.getMessage());
            throw new ServiceUnavailableException("catalog-service");
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> unwrap(Map<String, Object> body) {
        if (body == null) return null;
        Object data = body.get("data");
        if (data instanceof Map<?, ?> inner) return (Map<String, Object>) inner;
        return body;
    }

    public record VariantResponse(Long id, Long productId, String sku,
                                   BigDecimal price, int quantity, String status) {}
}

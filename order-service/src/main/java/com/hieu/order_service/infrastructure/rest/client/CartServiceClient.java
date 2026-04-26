package com.hieu.order_service.infrastructure.rest.client;

import com.hieu.order_service.domain.exception.EmptyCartException;
import com.hieu.order_service.domain.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * REST companion to {@link com.hieu.order_service.infrastructure.grpc.client.CartGrpcClient}.
 * Cart endpoints are JWT-protected — caller propagates the end-user's bearer token.
 */
@Component
@Slf4j
public class CartServiceClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestTemplate restTemplate;
    private final String cartServiceUrl;

    public CartServiceClient(RestTemplate restTemplate,
                              @Value("${services.cart-url:http://localhost:8084}") String cartServiceUrl) {
        this.restTemplate = restTemplate;
        this.cartServiceUrl = cartServiceUrl;
    }

    @SuppressWarnings("unchecked")
    public List<CartItemResponse> getCart(String authToken) {
        HttpHeaders headers = new HttpHeaders();
        if (authToken != null) headers.setBearerAuth(authToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    cartServiceUrl + "/api/cart", HttpMethod.GET, entity, MAP_TYPE);
            Map<String, Object> body = unwrap(resp.getBody());
            if (body == null) throw new ServiceUnavailableException("cart-service");
            Object items = body.get("items");
            if (!(items instanceof List<?> list) || list.isEmpty()) throw new EmptyCartException("Cart is empty");
            return list.stream()
                    .filter(e -> e instanceof Map<?, ?>)
                    .map(e -> (Map<String, Object>) e)
                    .map(CartServiceClient::toItem)
                    .toList();
        } catch (EmptyCartException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("REST cart.getCart failed: {}", e.getMessage());
            throw new ServiceUnavailableException("cart-service");
        }
    }

    public void clearCart(String authToken) {
        HttpHeaders headers = new HttpHeaders();
        if (authToken != null) headers.setBearerAuth(authToken);
        try {
            restTemplate.exchange(cartServiceUrl + "/api/cart",
                    HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        } catch (RestClientException e) {
            log.warn("REST cart.clearCart failed: {}", e.getMessage());
        }
    }

    private static CartItemResponse toItem(Map<String, Object> m) {
        return new CartItemResponse(
                m.get("productId") != null ? ((Number) m.get("productId")).longValue() : null,
                (String) m.get("productName"),
                m.get("variantId") != null ? ((Number) m.get("variantId")).longValue() : null,
                (String) m.get("variantSku"),
                (String) m.get("variantImage"),
                m.get("unitPrice") != null ? new BigDecimal(m.get("unitPrice").toString()) : BigDecimal.ZERO,
                m.get("quantity") != null ? ((Number) m.get("quantity")).intValue() : 0);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> unwrap(Map<String, Object> body) {
        if (body == null) return null;
        Object data = body.get("data");
        if (data instanceof Map<?, ?> inner) return (Map<String, Object>) inner;
        return body;
    }

    public record CartItemResponse(Long productId, String productName, Long variantId,
                                    String variantSku, String variantImage,
                                    BigDecimal unitPrice, int quantity) {}
}

package com.hieu.order_service.infrastructure.rest.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hieu.order_service.domain.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * REST client for voucher-service — validate (apply) + release.
 * Uses Spring 6 {@link RestClient} fluent API; behavior is identical to the former
 * {@link org.springframework.web.client.RestTemplate} version.
 *
 * <p>Validation errors (4xx from voucher-service: 404 not-found, 422 min-order/expired,
 * 409 limit-reached) surface as {@link VoucherInvalidException} so saga can mark order
 * FAILED with a clean reason instead of treating them as transport failures.
 */
@Component
@Slf4j
public class VoucherServiceClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String voucherServiceUrl;

    public VoucherServiceClient(@Qualifier("serviceRestClient") RestClient restClient,
                                ObjectMapper objectMapper,
                                @Value("${services.voucher-url:http://localhost:8094}") String voucherServiceUrl) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.voucherServiceUrl = voucherServiceUrl;
    }

    /**
     * Validate + atomically reserve a voucher slot. Returns the discount amount the
     * order should subtract from its subtotal.
     *
     * @param code         voucher code from order request
     * @param orderAmount  subtotal BEFORE discount (so voucher-service can check minOrder)
     * @param userId       order owner — for per-user usage limits
     * @param orderId      order id (numeric) — used as idempotency key for release later
     * @param productIds   variant/product ids in the cart — for product-restricted vouchers
     * @param authToken    end-user JWT (forwarded so gateway lets the call through)
     */
    public BigDecimal validateAndApply(String code, BigDecimal orderAmount, String userId,
                                       Long orderId, List<Long> productIds, String authToken) {
        // voucher-service DTO declares orderId:String + productIds:List<String> — encode
        // here so Jackson coercion config on the server side doesn't matter.
        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("code", code);
        body.put("orderAmount", orderAmount);
        body.put("userId", userId);
        body.put("orderId", String.valueOf(orderId));
        if (productIds != null && !productIds.isEmpty()) {
            body.put("productIds", productIds.stream().map(String::valueOf).toList());
        }

        try {
            var spec = restClient.post()
                    .uri(voucherServiceUrl + "/api/vouchers/validate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);

            if (authToken != null && !authToken.isBlank()) {
                String bearer = authToken.startsWith("Bearer ") ? authToken : "Bearer " + authToken;
                spec = spec.header(HttpHeaders.AUTHORIZATION, bearer);
            }

            Map<String, Object> payload = spec.retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                        // 4xx → voucher rejected. Read message from ApiResponse envelope.
                        // Don't log full response body — may contain PII (orderId/userId/amount).
                        log.warn("Voucher {} rejected: {}", code, resp.getStatusCode());
                        String message = extractMessage(resp);
                        throw new VoucherInvalidException(message, null);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                        throw new ServiceUnavailableException("voucher-service");
                    })
                    .body(MAP_TYPE);

            payload = unwrap(payload);
            if (payload == null || payload.get("discountAmount") == null) {
                throw new ServiceUnavailableException("voucher-service: missing discountAmount");
            }
            return new BigDecimal(payload.get("discountAmount").toString());

        } catch (VoucherInvalidException | ServiceUnavailableException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("REST voucher.validate({}) failed: {}", code, e.getMessage());
            throw new ServiceUnavailableException("voucher-service");
        }
    }

    /** Idempotent release. Voucher-service handles double-release silently. */
    public void release(String code, Long orderId) {
        try {
            restClient.post()
                    .uri(voucherServiceUrl + "/api/vouchers/release")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("code", code, "orderId", String.valueOf(orderId)))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Released voucher {} for order {}", code, orderId);
        } catch (RestClientException e) {
            // Compensation must not throw — voucher cleanup is best-effort. Voucher-service
            // also receives order.cancelled via Kafka so a transient failure here is recoverable.
            log.warn("REST voucher.release({}, {}) failed (will rely on Kafka): {}",
                    code, orderId, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> unwrap(Map<String, Object> b) {
        if (b == null) return null;
        Object data = b.get("data");
        return data instanceof Map<?, ?> ? (Map<String, Object>) data : b;
    }

    /** Pulls "message" out of voucher-service's ApiResponse error envelope via response body. */
    private String extractMessage(org.springframework.http.client.ClientHttpResponse resp) {
        try {
            byte[] bytes = resp.getBody().readAllBytes();
            var node = objectMapper.readTree(new String(bytes, StandardCharsets.UTF_8));
            var msg = node.path("message").asText(null);
            return (msg == null || msg.isBlank()) ? resp.getStatusCode().toString() : msg;
        } catch (Exception ignored) {
            try { return resp.getStatusCode().toString(); } catch (Exception e2) { return "voucher rejected"; }
        }
    }

    /** Thrown when voucher-service explicitly rejects (4xx) — saga maps to order FAILED. */
    public static class VoucherInvalidException extends RuntimeException {
        public VoucherInvalidException(String msg, Throwable cause) { super(msg, cause); }
    }
}

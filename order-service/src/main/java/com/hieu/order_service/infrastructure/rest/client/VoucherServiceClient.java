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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST client for voucher-service — validate (apply) + release.
 * Same RestTemplate + envelope-aware unwrap pattern as PaymentServiceClient.
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

    private final RestTemplate restTemplate;
    private final String voucherServiceUrl;

    public VoucherServiceClient(RestTemplate restTemplate,
                                @Value("${services.voucher-url:http://localhost:8094}") String voucherServiceUrl) {
        this.restTemplate = restTemplate;
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
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("orderAmount", orderAmount);
        body.put("userId", userId);
        body.put("orderId", orderId);
        if (productIds != null && !productIds.isEmpty()) body.put("productIds", productIds);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (authToken != null && !authToken.isBlank()) {
            String bearer = authToken.startsWith("Bearer ") ? authToken : "Bearer " + authToken;
            headers.set(HttpHeaders.AUTHORIZATION, bearer);
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    voucherServiceUrl + "/api/vouchers/validate", HttpMethod.POST, entity, MAP_TYPE);
            Map<String, Object> payload = unwrap(resp);
            if (payload == null || payload.get("discountAmount") == null) {
                throw new ServiceUnavailableException("voucher-service: missing discountAmount");
            }
            return new BigDecimal(payload.get("discountAmount").toString());

        } catch (HttpClientErrorException e) {
            // 4xx → voucher rejected (not found / min not met / expired / limit reached)
            log.warn("Voucher {} rejected: {} {}", code, e.getStatusCode(), e.getResponseBodyAsString());
            throw new VoucherInvalidException(extractMessage(e), e);
        } catch (RestClientException e) {
            log.error("REST voucher.validate({}) failed: {}", code, e.getMessage());
            throw new ServiceUnavailableException("voucher-service");
        }
    }

    /** Idempotent release. Voucher-service handles double-release silently. */
    public void release(String code, Long orderId) {
        Map<String, Object> body = Map.of("code", code, "orderId", orderId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            restTemplate.exchange(voucherServiceUrl + "/api/vouchers/release",
                    HttpMethod.POST, new HttpEntity<>(body, headers), MAP_TYPE);
            log.info("Released voucher {} for order {}", code, orderId);
        } catch (RestClientException e) {
            // Compensation must not throw — voucher cleanup is best-effort. Voucher-service
            // also receives order.cancelled via Kafka so a transient failure here is recoverable.
            log.warn("REST voucher.release({}, {}) failed (will rely on Kafka): {}",
                    code, orderId, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> unwrap(ResponseEntity<Map<String, Object>> resp) {
        Map<String, Object> b = resp.getBody();
        if (b == null) return null;
        Object data = b.get("data");
        return data instanceof Map<?, ?> ? (Map<String, Object>) data : b;
    }

    private static String extractMessage(HttpClientErrorException e) {
        try {
            // ApiResponse error envelope: {"success":false,"code":"VOUCHER-422","message":"..."}
            String body = e.getResponseBodyAsString();
            int idx = body.indexOf("\"message\"");
            if (idx < 0) return e.getStatusText();
            int colon = body.indexOf(':', idx);
            int q1 = body.indexOf('"', colon + 1);
            int q2 = body.indexOf('"', q1 + 1);
            return q1 >= 0 && q2 > q1 ? body.substring(q1 + 1, q2) : e.getStatusText();
        } catch (Exception ignored) {
            return e.getStatusText();
        }
    }

    /** Thrown when voucher-service explicitly rejects (4xx) — saga maps to order FAILED. */
    public static class VoucherInvalidException extends RuntimeException {
        public VoucherInvalidException(String msg, Throwable cause) { super(msg, cause); }
    }
}

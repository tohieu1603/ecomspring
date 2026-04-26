package com.hieu.order_service.infrastructure.rest.client;

import com.hieu.order_service.domain.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST client for payment-service — initiate + refund. Matches Base style:
 * {@link RestTemplate} + explicit {@link HttpEntity} + Bearer header when provided.
 * Transport errors surface as {@link ServiceUnavailableException} so the saga's
 * compensation path kicks in cleanly.
 */
@Component
@Slf4j
public class PaymentServiceClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestTemplate restTemplate;
    private final String paymentServiceUrl;

    public PaymentServiceClient(RestTemplate restTemplate,
                                 @Value("${services.payment-url:http://localhost:8086}") String paymentServiceUrl) {
        this.restTemplate = restTemplate;
        this.paymentServiceUrl = paymentServiceUrl;
    }

    /**
     * Initiate a payment. Returns paymentId + redirect info (QR for Sepay, URL for Momo).
     * Missing {@code paymentId} surfaces as a partial response — caller decides whether to
     * retry or continue.
     */
    public PaymentInitiated initiate(String orderId, BigDecimal amount, String method,
                                      String idempotencyKey, String authToken) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderId", orderId);
        body.put("amount", amount);
        body.put("currency", "VND");
        body.put("method", method);
        body.put("idempotencyKey", idempotencyKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (idempotencyKey != null) headers.set("X-Idempotency-Key", idempotencyKey);
        // payment-service's /api/payments is JWT-protected — propagate the end-user's
        // token from the saga so the auth context flows downstream.
        if (authToken != null && !authToken.isBlank()) {
            String bearer = authToken.startsWith("Bearer ") ? authToken : "Bearer " + authToken;
            headers.set(HttpHeaders.AUTHORIZATION, bearer);
        }
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    paymentServiceUrl + "/api/payments", HttpMethod.POST, entity, MAP_TYPE);
            Map<String, Object> payload = unwrap(resp);
            if (payload == null) throw new ServiceUnavailableException("payment-service");
            Long paymentId = payload.get("paymentId") != null
                    ? Long.parseLong(payload.get("paymentId").toString()) : null;
            return new PaymentInitiated(paymentId,
                    (String) payload.get("qrCodeUrl"), (String) payload.get("payUrl"));
        } catch (RestClientException e) {
            log.error("REST payment.initiate({}) failed: {}", orderId, e.getMessage());
            throw new ServiceUnavailableException("payment-service");
        }
    }

    /**
     * Process a refund for the payment bound to {@code orderId}. Two-step flow mirroring
     * Base: first resolve paymentId via {@code /order/{id}}, then POST process-refund.
     * No-ops (logged) if no payment exists yet.
     */
    public void processRefundForOrder(String orderId, BigDecimal refundAmount, String adminToken) {
        HttpHeaders adminHeaders = buildAdminHeaders(adminToken);
        HttpEntity<Void> lookupEntity = new HttpEntity<>(adminHeaders);

        try {
            ResponseEntity<Map<String, Object>> lookup = restTemplate.exchange(
                    paymentServiceUrl + "/api/payments/order/" + orderId,
                    HttpMethod.GET, lookupEntity, MAP_TYPE);
            Map<String, Object> payment = unwrap(lookup);
            if (payment == null || payment.get("id") == null) {
                log.warn("No payment found for orderId={} — skipping refund", orderId);
                return;
            }
            long paymentId = ((Number) payment.get("id")).longValue();

            Map<String, Object> body = new LinkedHashMap<>();
            if (refundAmount != null) body.put("refundAmount", refundAmount);
            body.put("reason", "order-refund");
            HttpEntity<Map<String, Object>> refundEntity = new HttpEntity<>(body, adminHeaders);

            restTemplate.exchange(
                    paymentServiceUrl + "/api/payments/" + paymentId + "/process-refund",
                    HttpMethod.POST, refundEntity, MAP_TYPE);
            log.info("Refund processed for orderId={}, paymentId={}", orderId, paymentId);
        } catch (RestClientException e) {
            log.error("REST payment.processRefund({}) failed: {}", orderId, e.getMessage());
            throw new ServiceUnavailableException("payment-service");
        }
    }

    /**
     * Unwrap both bare DTOs and {@code ApiResponse}-wrapped responses. Our services return
     * raw DTOs today, but this keeps us compatible if we add the common-lib envelope later.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> unwrap(ResponseEntity<Map<String, Object>> resp) {
        if (resp.getStatusCode() != HttpStatus.OK && resp.getStatusCode() != HttpStatus.CREATED) return null;
        Map<String, Object> body = resp.getBody();
        if (body == null) return null;
        Object data = body.get("data");
        if (data instanceof Map<?, ?> inner) return (Map<String, Object>) inner;
        return body;
    }

    private static HttpHeaders buildAdminHeaders(String adminToken) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (adminToken != null && !adminToken.isBlank()) h.setBearerAuth(adminToken);
        return h;
    }

    public record PaymentInitiated(Long paymentId, String qrCodeUrl, String payUrl) {}
}

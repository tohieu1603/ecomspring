package com.hieu.order_service.infrastructure.rest.client;

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
import java.util.Map;

/**
 * REST client for payment-service — initiate + refund.
 * Uses Spring 6 {@link RestClient} fluent API; behavior is identical to the former
 * {@link org.springframework.web.client.RestTemplate} version.
 * Transport errors surface as {@link ServiceUnavailableException} so the saga's
 * compensation path kicks in cleanly.
 */
@Component
@Slf4j
public class PaymentServiceClient {

    private static final String AUTH_BEARER_PREFIX = "Bearer ";
    private static final String SERVICE_NAME = "payment-service";


    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final String paymentServiceUrl;

    public PaymentServiceClient(@Qualifier("serviceRestClient") RestClient restClient,
                                @Value("${services.payment-url:http://localhost:8086}") String paymentServiceUrl) {
        this.restClient = restClient;
        this.paymentServiceUrl = paymentServiceUrl;
    }

    /**
     * Initiate a payment. Returns paymentId + redirect info (QR for Sepay, URL for Momo).
     * Missing {@code paymentId} surfaces as a partial response — caller decides whether to
     * retry or continue.
     */
    public PaymentInitiated initiate(String orderId, BigDecimal amount, String method,
                                     String idempotencyKey, String authToken) {
        try {
            var initiateBody = new java.util.LinkedHashMap<String, Object>();
            initiateBody.put("orderId", orderId);
            initiateBody.put("amount", amount);
            initiateBody.put("currency", "VND");
            initiateBody.put("method", method);
            if (idempotencyKey != null) initiateBody.put("idempotencyKey", idempotencyKey);

            var spec = restClient.post()
                    .uri(paymentServiceUrl + "/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(initiateBody);

            if (idempotencyKey != null) spec = spec.header("X-Idempotency-Key", idempotencyKey);
            // payment-service's /api/payments is JWT-protected — propagate the end-user's
            // token from the saga so the auth context flows downstream.
            if (authToken != null && !authToken.isBlank()) {
                String bearer = authToken.startsWith(AUTH_BEARER_PREFIX) ? authToken : AUTH_BEARER_PREFIX + authToken;
                spec = spec.header(HttpHeaders.AUTHORIZATION, bearer);
            }

            Map<String, Object> payload = spec.retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        throw new ServiceUnavailableException(SERVICE_NAME);
                    })
                    .body(MAP_TYPE);

            payload = unwrap(payload);
            if (payload == null) throw new ServiceUnavailableException(SERVICE_NAME);

            Long paymentId = payload.get("paymentId") != null
                    ? Long.parseLong(payload.get("paymentId").toString()) : null;
            return new PaymentInitiated(paymentId,
                    (String) payload.get("qrCodeUrl"), (String) payload.get("payUrl"));
        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("REST payment.initiate({}) failed: {}", orderId, e.getMessage());
            throw new ServiceUnavailableException(SERVICE_NAME);
        }
    }

    /**
     * Process a refund for the payment bound to {@code orderId}. Two-step flow:
     * first resolve paymentId via {@code /order/{id}}, then POST process-refund.
     * No-ops (logged) if no payment exists yet.
     */
    public void processRefundForOrder(String orderId, BigDecimal refundAmount, String adminToken) {
        String bearer = resolveBearerHeader(adminToken);
        try {
            // Step 1: look up payment by orderId
            var lookupSpec = restClient.get()
                    .uri(paymentServiceUrl + "/api/payments/order/" + orderId);
            if (bearer != null) lookupSpec = lookupSpec.header(HttpHeaders.AUTHORIZATION, bearer);

            Map<String, Object> payment = lookupSpec.retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        throw new ServiceUnavailableException(SERVICE_NAME);
                    })
                    .body(MAP_TYPE);

            payment = unwrap(payment);
            if (payment == null || payment.get("id") == null) {
                log.warn("No payment found for orderId={} — skipping refund", orderId);
                return;
            }
            long paymentId = ((Number) payment.get("id")).longValue();

            // Step 2: post refund
            var refundBodyBuilder = new java.util.LinkedHashMap<String, Object>();
            if (refundAmount != null) refundBodyBuilder.put("refundAmount", refundAmount);
            refundBodyBuilder.put("reason", "order-refund");

            var refundSpec = restClient.post()
                    .uri(paymentServiceUrl + "/api/payments/" + paymentId + "/process-refund")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(refundBodyBuilder);
            if (bearer != null) refundSpec = refundSpec.header(HttpHeaders.AUTHORIZATION, bearer);

            refundSpec.retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        throw new ServiceUnavailableException(SERVICE_NAME);
                    })
                    .toBodilessEntity();

            log.info("Refund processed for orderId={}, paymentId={}", orderId, paymentId);
        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("REST payment.processRefund({}) failed: {}", orderId, e.getMessage());
            throw new ServiceUnavailableException(SERVICE_NAME);
        }
    }

    /**
     * Unwrap both bare DTOs and {@code ApiResponse}-wrapped responses. Our services return
     * raw DTOs today, but this keeps us compatible if we add the common-lib envelope later.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> unwrap(Map<String, Object> body) {
        if (body == null) return null;
        Object data = body.get("data");
        if (data instanceof Map<?, ?> inner) return (Map<String, Object>) inner;
        return body;
    }

    private static String resolveBearerHeader(String token) {
        if (token == null || token.isBlank()) return null;
        return token.startsWith(AUTH_BEARER_PREFIX) ? token : AUTH_BEARER_PREFIX + token;
    }

    public record PaymentInitiated(Long paymentId, String qrCodeUrl, String payUrl) {}
}

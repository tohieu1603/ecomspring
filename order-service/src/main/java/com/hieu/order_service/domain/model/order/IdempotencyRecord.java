package com.hieu.order_service.domain.model.order;

import lombok.Getter;

import java.time.Duration;
import java.time.Instant;

/** Persistent idempotency record — prevents duplicate order creation. */
@Getter
public class IdempotencyRecord {

    public enum Status { PROCESSING, COMPLETED, FAILED }

    private final String idempotencyKey;
    private Long orderId;
    private Status status;
    private String responseBody;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final Instant processingStartedAt;

    private IdempotencyRecord(String idempotencyKey, Status status, Long orderId,
                               String responseBody, Instant createdAt, Instant expiresAt,
                               Instant processingStartedAt) {
        this.idempotencyKey = idempotencyKey;
        this.orderId = orderId;
        this.status = status;
        this.responseBody = responseBody;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.processingStartedAt = processingStartedAt;
    }

    public static IdempotencyRecord create(String key) {
        var now = Instant.now();
        return new IdempotencyRecord(key, Status.PROCESSING, null, null, now, now.plus(Duration.ofMinutes(30)), now);
    }

    public static IdempotencyRecord reconstitute(String key, Long orderId, Status status,
                                                  String responseBody, Instant createdAt,
                                                  Instant expiresAt, Instant processingStartedAt) {
        return new IdempotencyRecord(key, status, orderId, responseBody, createdAt, expiresAt, processingStartedAt);
    }

    public void markCompleted(Long orderId, String json) {
        this.orderId = orderId;
        this.status = Status.COMPLETED;
        this.responseBody = json;
    }

    public void markFailed(String reason) {
        this.status = Status.FAILED;
        this.responseBody = reason;
    }

    public boolean isExpired() { return Instant.now().isAfter(expiresAt); }
}

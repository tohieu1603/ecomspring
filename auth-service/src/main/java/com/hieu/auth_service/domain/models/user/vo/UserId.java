package com.hieu.auth_service.domain.models.user.vo;


import com.hieu.auth_service.domain.models.refreshtoken.vo.TokenId;

import java.util.UUID;

/**
 * Value Object representing a User ID.
 * Implemented as a record to ensure immutability.
 */
public record UserId(String value) {

    // ── Compact Constructor ──
    public UserId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("UserId cannot be null or empty");
        }
        // Chuẩn hóa loại bỏ khoảng trắng thừa
        value = value.trim();
    }

    public static UserId of(String value) {
        return new UserId(value);
    }
    public static UserId generate() {
        return new UserId(UUID.randomUUID().toString());
    }
    @Override
    public String toString() {
        return value;
    }
}
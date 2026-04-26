package com.hieu.auth_service.domain.models.role.vo;

import com.hieu.auth_service.domain.models.refreshtoken.vo.TokenId;

import java.util.UUID;

/**
 * Value Object representing Role Identity
 */
public record RoleId(String value) {

    public RoleId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Role id cannot be null or empty");
        }
    }
    public static RoleId generate() {
        return new RoleId(UUID.randomUUID().toString());
    }
    public static RoleId of(String value) {
        return new RoleId(value);
    }
}
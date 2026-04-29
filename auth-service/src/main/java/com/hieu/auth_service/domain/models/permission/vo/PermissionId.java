package com.hieu.auth_service.domain.models.permission.vo;

import java.util.UUID;

public record PermissionId(String value) {

    public PermissionId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Permission id cannot be empty");
        }
    }
    public static PermissionId generate() {
        return new PermissionId(UUID.randomUUID().toString());
    }
    public static PermissionId of(String value) {
        return new PermissionId(value);
    }
}
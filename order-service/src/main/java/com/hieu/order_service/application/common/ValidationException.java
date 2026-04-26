package com.hieu.order_service.application.common;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ValidationException extends ApplicationException {

    private final Map<String, String> fieldErrors;

    public ValidationException(String message) {
        super("APP-400", message);
        this.fieldErrors = new LinkedHashMap<>();
    }

    public ValidationException(String message, Map<String, String> fieldErrors) {
        super("APP-400", message);
        this.fieldErrors = fieldErrors == null ? new LinkedHashMap<>() : new LinkedHashMap<>(fieldErrors);
    }

    public Map<String, String> fieldErrors() { return Map.copyOf(fieldErrors); }
    public boolean hasFieldErrors() { return !fieldErrors.isEmpty(); }
}

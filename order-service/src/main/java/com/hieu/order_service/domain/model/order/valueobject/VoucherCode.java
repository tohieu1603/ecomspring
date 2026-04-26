package com.hieu.order_service.domain.model.order.valueobject;

public record VoucherCode(String value) {

    public static VoucherCode of(String value) {
        return value == null || value.isBlank() ? null : new VoucherCode(value.trim().toUpperCase());
    }
}

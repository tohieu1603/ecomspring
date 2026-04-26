package com.hieu.order_service.interfaces.rest;

public record CreateOrderFromCartRequest(
        String recipientName,
        String recipientPhone,
        String street,
        String ward,
        String district,
        String city,
        String country,
        String postalCode,
        String paymentMethod,
        String notes,
        String voucherCode
) {}

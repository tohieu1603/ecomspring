package com.hieu.order_service.domain.exception;

/** Stable error-code catalogue for order-service domain failures. Codes use {@code ORDER-NNNN} format. */
public final class OrderErrorCodes {

    private OrderErrorCodes() {}

    // Order (3000-3099)
    public static final String ORDER_NOT_FOUND          = "ORDER-3001";
    public static final String ORDER_ALREADY_EXISTS     = "ORDER-3002";
    public static final String ORDER_INVALID_STATE      = "ORDER-3003";
    public static final String ORDER_INSUFFICIENT_STOCK = "ORDER-3004";
    public static final String ORDER_DUPLICATE          = "ORDER-3005";
    public static final String ORDER_SERVICE_UNAVAILABLE= "ORDER-3006";
    public static final String ORDER_CANCELLED          = "ORDER-3007";
    public static final String ORDER_EMPTY_CART         = "ORDER-3008";

    // Return request (3100-3199)
    public static final String RETURN_REQUEST_NOT_FOUND = "ORDER-3101";
    public static final String RETURN_REQUEST_INVALID   = "ORDER-3102";
}

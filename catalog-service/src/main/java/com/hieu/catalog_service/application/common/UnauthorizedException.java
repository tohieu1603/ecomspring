package com.hieu.catalog_service.application.common;

/** Thrown when the authenticated caller lacks permission for a command. */
public final class UnauthorizedException extends ApplicationException {
    public UnauthorizedException(String message) {
        super("APP-403", message);
    }
}

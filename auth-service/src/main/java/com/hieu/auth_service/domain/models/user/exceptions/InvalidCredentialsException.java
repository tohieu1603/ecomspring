package com.hieu.auth_service.domain.models.user.exceptions;

import com.hieu.auth_service.domain.shared.DomainException;

public final class InvalidCredentialsException extends DomainException {
    public InvalidCredentialsException() {
        super("AUTH-1001", "Invalid username or password");
    }
}

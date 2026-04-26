package com.hieu.auth_service.domain.models.user.exceptions;

import com.hieu.auth_service.domain.shared.DomainException;

public final class UserAlreadyExistsException extends DomainException {
    public UserAlreadyExistsException(String reason) {
        super("AUTH-1002", "User already exists: " + reason);
    }
}

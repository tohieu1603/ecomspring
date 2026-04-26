package com.hieu.auth_service.domain.models.user.exceptions;

import com.hieu.auth_service.domain.shared.DomainException;

public final class UserNotFoundException extends DomainException {
    public UserNotFoundException(String lookup) {
        super("AUTH-1003", "User not found: " + lookup);
    }
}

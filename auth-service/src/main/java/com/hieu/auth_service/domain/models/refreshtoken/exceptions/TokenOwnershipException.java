package com.hieu.auth_service.domain.models.refreshtoken.exceptions;

import com.hieu.auth_service.domain.shared.DomainException;

public final class TokenOwnershipException extends DomainException {
    public TokenOwnershipException(String userId) {
        super("AUTH-1013", "Token does not belong to user: " + userId);
    }
}

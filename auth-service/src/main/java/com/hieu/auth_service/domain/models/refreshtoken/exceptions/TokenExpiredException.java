package com.hieu.auth_service.domain.models.refreshtoken.exceptions;

import com.hieu.auth_service.domain.shared.DomainException;

public final class TokenExpiredException extends DomainException {
    private final String tokenId;

    public TokenExpiredException(String tokenId) {
        super("AUTH-1004", "Refresh token has expired");
        this.tokenId = tokenId;
    }

    public String tokenId() { return tokenId; }
}

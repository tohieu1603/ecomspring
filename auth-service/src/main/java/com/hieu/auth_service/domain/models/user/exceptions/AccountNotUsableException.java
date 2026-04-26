package com.hieu.auth_service.domain.models.user.exceptions;

import com.hieu.auth_service.domain.shared.DomainException;

/**
 * Thrown when an otherwise authenticated user cannot log in due to account-state flags.
 * A single exception with a {@link Reason} beats four near-identical classes — the reason
 * carries the distinct error code so clients can still branch per-case.
 */
public final class AccountNotUsableException extends DomainException {

    public enum Reason {
        DISABLED("AUTH-1007", "Account is disabled"),
        LOCKED("AUTH-1006", "Account is locked"),
        EXPIRED("AUTH-1014", "Account has expired"),
        CREDENTIALS_EXPIRED("AUTH-1015", "Credentials have expired");

        private final String code;
        private final String message;

        Reason(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String code()    { return code; }
        public String message() { return message; }
    }

    private final Reason reason;

    public AccountNotUsableException(Reason reason) {
        super(reason.code(), reason.message());
        this.reason = reason;
    }

    public Reason reason() { return reason; }
}

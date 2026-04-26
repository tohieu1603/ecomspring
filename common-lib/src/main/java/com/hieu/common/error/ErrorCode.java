package com.hieu.common.error;

import org.springframework.http.HttpStatus;

/**
 * Canonical error codes exposed across services.
 *
 * <p>Stable machine-readable identifiers that clients can branch on without parsing
 * human-readable messages. Naming convention: {@code <DOMAIN>-<NNNN>}; once assigned,
 * codes are never renamed — only added. HTTP status mapping is baked in so services
 * can translate consistently at the web boundary.
 */
public enum ErrorCode {

    // ── Generic (COMMON) ────────────────────────────────────────────────
    INTERNAL_ERROR         ("COMMON-500", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_FAILED      ("COMMON-400", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED           ("COMMON-401", HttpStatus.UNAUTHORIZED),
    FORBIDDEN              ("COMMON-403", HttpStatus.FORBIDDEN),
    NOT_FOUND              ("COMMON-404", HttpStatus.NOT_FOUND),
    CONFLICT               ("COMMON-409", HttpStatus.CONFLICT),
    SERVICE_UNAVAILABLE    ("COMMON-503", HttpStatus.SERVICE_UNAVAILABLE),

    // ── Auth domain ─────────────────────────────────────────────────────
    INVALID_CREDENTIALS    ("AUTH-1001", HttpStatus.UNAUTHORIZED),
    USER_ALREADY_EXISTS    ("AUTH-1002", HttpStatus.CONFLICT),
    USER_NOT_FOUND         ("AUTH-1003", HttpStatus.NOT_FOUND),
    TOKEN_EXPIRED          ("AUTH-1004", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID          ("AUTH-1005", HttpStatus.UNAUTHORIZED),
    ACCOUNT_LOCKED         ("AUTH-1006", HttpStatus.FORBIDDEN),
    ACCOUNT_DISABLED       ("AUTH-1007", HttpStatus.FORBIDDEN),
    REFRESH_TOKEN_INVALID  ("AUTH-1008", HttpStatus.UNAUTHORIZED),
    ROLE_NOT_FOUND         ("AUTH-1009", HttpStatus.NOT_FOUND),
    PERMISSION_NOT_FOUND   ("AUTH-1010", HttpStatus.NOT_FOUND),
    TOKEN_REVOKED          ("AUTH-1011", HttpStatus.UNAUTHORIZED),
    TOKEN_REUSE_DETECTED   ("AUTH-1012", HttpStatus.UNAUTHORIZED),
    TOKEN_OWNERSHIP_FAIL   ("AUTH-1013", HttpStatus.FORBIDDEN);

    private final String code;
    private final HttpStatus httpStatus;

    ErrorCode(String code, HttpStatus httpStatus) {
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String code()          { return code; }
    public HttpStatus httpStatus() { return httpStatus; }
}

package com.hieu.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Stateless JWT validation helper shared across services.
 *
 * <p>Token <em>issuance</em> stays in auth-service (single source of truth); every other
 * service just verifies incoming tokens via this class. The same HS256 secret must be
 * distributed to every consumer — in production, inject via secret manager / Vault.
 *
 * <p>Deliberately framework-free: no Spring beans, no Lombok — easier to re-use in
 * reactive gateway code, CLIs, or Kotlin scripts.
 */
public class JwtTokenValidator {

    private final SecretKey secretKey;

    /**
     * Builds the validator from a raw shared secret. Secret length must satisfy HS256
     * (≥ 32 bytes). Weak secrets fail fast in the {@link Keys#hmacShaKeyFor} call.
     *
     * @param secret shared HMAC secret
     */
    public JwtTokenValidator(String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Builds the validator from a pre-constructed secret key (e.g. for tests). */
    public JwtTokenValidator(SecretKey secretKey) {
        this.secretKey = secretKey;
    }

    /**
     * Signature + basic structural validation only. Expiry is checked separately so
     * callers can distinguish "forged" from "naturally expired" responses.
     *
     * @param token raw JWT string
     * @return {@code true} when the signature verifies and the token is well-formed
     */
    public boolean validateSignature(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Parses all claims; throws {@link JwtException} on invalid tokens.
     *
     * @param token raw JWT string
     * @return parsed claims
     */
    public Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    }

    /** @return JWT {@code sub} claim (username). */
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /** @return {@code userId} claim (UUID String — matches auth-service's domain). */
    public String extractUserId(String token) {
        return parseClaims(token).get("userId", String.class);
    }

    /** @return {@code jti} claim — used as blacklist key. */
    public String extractTokenId(String token) {
        return parseClaims(token).getId();
    }

    /** @return {@code tokenVersion} claim, or {@code 0} when absent (legacy tokens). */
    public int extractTokenVersion(String token) {
        Integer version = parseClaims(token).get("tokenVersion", Integer.class);
        return version != null ? version : 0;
    }

    /** @return {@code roles} claim; empty list when absent. */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        List<String> roles = parseClaims(token).get("roles", List.class);
        return roles != null ? List.copyOf(roles) : List.of();
    }

    /**
     * @param token raw JWT string
     * @return {@code true} when the token's exp claim is in the past (treats unparseable as expired)
     */
    public boolean isExpired(String token) {
        try {
            return parseClaims(token).getExpiration().before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }
}

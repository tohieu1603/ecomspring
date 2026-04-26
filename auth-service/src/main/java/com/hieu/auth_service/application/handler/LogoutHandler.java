package com.hieu.auth_service.application.handler;

import com.hieu.auth_service.application.command.LogoutCommand;
import com.hieu.auth_service.application.common.CommandHandler;
import com.hieu.auth_service.application.port.TokenBlacklistPort;
import com.hieu.auth_service.domain.models.refreshtoken.RefreshToken;
import com.hieu.auth_service.domain.models.refreshtoken.vo.RevokedReason;
import com.hieu.auth_service.domain.models.refreshtoken.vo.TokenValue;
import com.hieu.auth_service.domain.repositories.RefreshTokenRepository;
import com.hieu.auth_service.domain.services.TokenProviderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles {@link LogoutCommand}: revokes the refresh token and blacklists the access token
 * so subsequent requests with the same credentials fail immediately.
 */
@Service
@RequiredArgsConstructor
public class LogoutHandler implements CommandHandler<LogoutCommand, Void> {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenProviderPort tokenProvider;
    private final TokenBlacklistPort tokenBlacklist;

    @Override
    @Transactional
    public Void handle(LogoutCommand command) {
        // Step 1: revoke the refresh token (idempotent — missing token simply means the user is already out).
        refreshTokenRepository.findByTokenValue(TokenValue.of(command.refreshToken()))
                .ifPresent(this::revokeRefresh);

        // Step 2: blacklist the access token by its jti until natural expiry.
        if (command.accessToken() != null && !command.accessToken().isBlank()) {
            var claims = tokenProvider.parseAccessToken(command.accessToken());
            tokenBlacklist.revoke(claims.tokenId(), claims.userId(), claims.expiresAt(), "LOGOUT");
        }
        return null;
    }

    private void revokeRefresh(RefreshToken t) {
        t.revoke(RevokedReason.USER_INITIATED);
        refreshTokenRepository.save(t);
    }
}

package com.hieu.auth_service.application.handler;

import com.hieu.auth_service.application.command.ChangePasswordCommand;
import com.hieu.auth_service.application.common.CommandHandler;
import com.hieu.auth_service.domain.models.user.User;
import com.hieu.auth_service.domain.models.user.exceptions.UserNotFoundException;
import com.hieu.auth_service.domain.models.user.vo.Password;
import com.hieu.auth_service.domain.models.user.vo.UserId;
import com.hieu.auth_service.domain.repositories.RefreshTokenRepository;
import com.hieu.auth_service.domain.repositories.UserRepository;
import com.hieu.auth_service.domain.services.PasswordEncoderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles {@link ChangePasswordCommand}.
 *
 * <p>Delegates credential validation + token-version bump to the {@code User} aggregate,
 * then bulk-revokes all refresh tokens so every outstanding session is invalidated.
 */
@Service
@RequiredArgsConstructor
public class ChangePasswordHandler implements CommandHandler<ChangePasswordCommand, Void> {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoderPort passwordEncoder;

    @Override
    @Transactional
    public Void handle(ChangePasswordCommand command) {
        User user = userRepository.findById(UserId.of(command.userId()))
                .orElseThrow(() -> new UserNotFoundException(command.userId()));

        user.changePassword(
                Password.createRaw(command.oldRawPassword()),
                Password.createRaw(command.newRawPassword()),
                passwordEncoder);

        userRepository.save(user);
        refreshTokenRepository.revokeAllTokensForUser(user.getId());
        return null;
    }
}

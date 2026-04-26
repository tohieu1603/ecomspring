package com.hieu.auth_service.application.handler;

import com.hieu.auth_service.application.command.ChangeAccountStatusCommand;
import com.hieu.auth_service.application.common.CommandHandler;
import com.hieu.auth_service.domain.models.user.User;
import com.hieu.auth_service.domain.models.user.exceptions.UserNotFoundException;
import com.hieu.auth_service.domain.models.user.vo.UserId;
import com.hieu.auth_service.domain.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles {@link ChangeAccountStatusCommand}. Dispatches to the aggregate method matching
 * the transition enum — a small switch keeps handler count low while each transition
 * still raises its own domain event via {@link User}'s status mutators.
 */
@Service
@RequiredArgsConstructor
public class ChangeAccountStatusHandler implements CommandHandler<ChangeAccountStatusCommand, Void> {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public Void handle(ChangeAccountStatusCommand command) {
        User user = userRepository.findById(UserId.of(command.userId()))
                .orElseThrow(() -> new UserNotFoundException(command.userId()));

        switch (command.transition()) {
            case LOCK    -> user.lock();
            case UNLOCK  -> user.unlock();
            case ENABLE  -> user.enable();
            case DISABLE -> user.disable();
        }

        userRepository.save(user);
        return null;
    }
}

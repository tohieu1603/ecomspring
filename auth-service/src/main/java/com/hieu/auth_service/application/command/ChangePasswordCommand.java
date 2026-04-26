package com.hieu.auth_service.application.command;

import com.hieu.auth_service.application.common.Command;

/**
 * Changes the current user's password.
 *
 * <p>Side effects enforced by the handler:
 * <ul>
 *   <li>Verifies the old password.</li>
 *   <li>Bumps {@code tokenVersion}, invalidating every existing access token.</li>
 *   <li>Revokes all outstanding refresh tokens for the user.</li>
 * </ul>
 *
 * @param userId         subject user id (from authenticated context)
 * @param oldRawPassword user's current password
 * @param newRawPassword desired new password
 */
public record ChangePasswordCommand(
        String userId,
        String oldRawPassword,
        String newRawPassword
) implements Command<Void> {
}

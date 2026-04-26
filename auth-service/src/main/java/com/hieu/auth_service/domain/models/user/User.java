package com.hieu.auth_service.domain.models.user;

import com.hieu.auth_service.domain.models.role.vo.RoleId;
import com.hieu.auth_service.domain.models.user.events.*;
import com.hieu.auth_service.domain.models.user.exceptions.AccountNotUsableException;
import com.hieu.auth_service.domain.models.user.vo.*;
import com.hieu.auth_service.domain.services.PasswordEncoderPort;
import com.hieu.auth_service.domain.shared.AggregateRoot;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * User aggregate root.
 *
 * <p>Encapsulates identity, credentials, account status, role assignments, and token
 * versioning. State transitions go through explicit business methods that enforce
 * invariants and raise {@link com.hieu.auth_service.domain.events.DomainEvent}s.
 * Event management is inherited from {@link AggregateRoot}; events are drained by
 * infrastructure via {@code pullDomainEvents()} after a successful save.
 */
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(of = {"id", "username", "email", "accountStatus"})
public final class User extends AggregateRoot {

    @EqualsAndHashCode.Include
    private UserId id;
    private Username username;
    private Email email;
    private Password password;
    private PersonName personName;
    private AccountStatus accountStatus;
    private Set<RoleId> roles;
    private int tokenVersion;
    private Instant createdAt;
    private Instant updatedAt;

    private User() {
        this.roles = new HashSet<>();
    }

    // ── Factories ──────────────────────────────────────────────────────────

    /**
     * Registers a brand-new user. The raw password is hashed here via the encoder port
     * — plaintext never reaches persistence.
     */
    public static User register(Username username, Email email, Password rawPassword,
                                PersonName personName, PasswordEncoderPort encoder) {
        Objects.requireNonNull(encoder, "encoder");
        if (!rawPassword.needsEncoding()) {
            throw new IllegalArgumentException("register() requires a raw password");
        }

        User u = new User();
        u.id = UserId.generate();
        u.username = username;
        u.email = email;
        u.password = Password.createEncoded(encoder.encode(rawPassword.value()));
        u.personName = personName;
        u.accountStatus = AccountStatus.createActive();
        u.tokenVersion = 1;
        u.createdAt = Instant.now();
        u.updatedAt = u.createdAt;

        u.registerEvent(new UserCreatedEvent(u.id.value(), username.value(), email.value()));
        return u;
    }

    /** Rebuilds an aggregate from persistent state — used only by repositories. */
    public static User reconstitute(UserId id, Username username, Email email, Password password,
                                    PersonName personName, AccountStatus status, Set<RoleId> roles,
                                    int tokenVersion, Instant createdAt, Instant updatedAt) {
        User u = new User();
        u.id = id;
        u.username = username;
        u.email = email;
        u.password = password;
        u.personName = personName;
        u.accountStatus = status;
        u.roles = new HashSet<>(roles);
        u.tokenVersion = tokenVersion;
        u.createdAt = createdAt;
        u.updatedAt = updatedAt;
        return u;
    }

    // ── Authentication ────────────────────────────────────────────────────

    /**
     * Verifies credentials and records the login. Throws {@link AccountNotUsableException}
     * if the account is currently disabled/locked/expired — callers differentiate between
     * "wrong password" and "unusable account" via distinct exception codes.
     */
    public boolean authenticate(Password rawPassword, PasswordEncoderPort encoder) {
        ensureAuthenticatable();

        boolean matches = encoder.matches(rawPassword.value(), password.value());
        if (matches) {
            recordLogin();
        }
        return matches;
    }

    private void ensureAuthenticatable() {
        var s = accountStatus;
        if (!s.enabled())               throw new AccountNotUsableException(AccountNotUsableException.Reason.DISABLED);
        if (!s.accountNonLocked())      throw new AccountNotUsableException(AccountNotUsableException.Reason.LOCKED);
        if (!s.accountNonExpired())     throw new AccountNotUsableException(AccountNotUsableException.Reason.EXPIRED);
        if (!s.credentialsNonExpired()) throw new AccountNotUsableException(AccountNotUsableException.Reason.CREDENTIALS_EXPIRED);
    }

    private void recordLogin() {
        Instant now = Instant.now();
        accountStatus = accountStatus.withLastLogin(now);
        updatedAt = now;
        registerEvent(new UserLoggedInEvent(id.value(), username.value()));
    }

    // ── Credentials ───────────────────────────────────────────────────────

    /**
     * Changes the password after verifying the old one and invalidates all outstanding
     * JWTs by bumping {@link #tokenVersion}.
     */
    public void changePassword(Password oldPassword, Password newPassword, PasswordEncoderPort encoder) {
        if (!encoder.matches(oldPassword.value(), password.value())) {
            throw new IllegalStateException("Old password is incorrect");
        }

        password = newPassword.needsEncoding()
                ? Password.createEncoded(encoder.encode(newPassword.value()))
                : newPassword;

        incrementTokenVersion();
        registerEvent(new PasswordChangedEvent(id.value(), username.value()));
    }

    public void updateEmail(Email newEmail) {
        if (email.equals(newEmail)) return;
        Email oldEmail = email;
        email = newEmail;
        updatedAt = Instant.now();
        registerEvent(new EmailChangedEvent(id.value(), oldEmail.value(), newEmail.value()));
    }

    public void updatePersonName(PersonName newName) {
        if (personName.equals(newName)) return;
        personName = newName;
        updatedAt = Instant.now();
    }

    // ── Account-status transitions ────────────────────────────────────────

    public void lock()    { transitionStatus(accountStatus.lock(),    AccountStatusChangedEvent.Transition.LOCKED); }
    public void unlock()  { transitionStatus(accountStatus.unlock(),  AccountStatusChangedEvent.Transition.UNLOCKED); }
    public void disable() { transitionStatus(accountStatus.disable(), AccountStatusChangedEvent.Transition.DISABLED); }
    public void enable()  { transitionStatus(accountStatus.enable(),  AccountStatusChangedEvent.Transition.ENABLED); }

    private void transitionStatus(AccountStatus next, AccountStatusChangedEvent.Transition kind) {
        if (accountStatus.equals(next)) return;
        accountStatus = next;
        updatedAt = Instant.now();
        registerEvent(new AccountStatusChangedEvent(id.value(), username.value(), kind));
    }

    public boolean isActive() {
        return accountStatus.isActive();
    }

    // ── Role management ──────────────────────────────────────────────────

    public void assignRole(RoleId roleId) {
        if (roles.add(roleId)) {
            updatedAt = Instant.now();
            registerEvent(new RoleAssignedEvent(id.value(), roleId.value()));
        }
    }

    public void unassignRole(RoleId roleId) {
        if (roles.remove(roleId)) {
            updatedAt = Instant.now();
            registerEvent(new RoleRemovedEvent(id.value(), roleId.value()));
        }
    }

    public boolean hasRole(RoleId roleId) { return roles.contains(roleId); }

    /** Unmodifiable view of assigned role ids. */
    public Set<RoleId> getRoles() { return Collections.unmodifiableSet(roles); }

    // ── Token version ────────────────────────────────────────────────────

    /**
     * Bumps the token version to invalidate every outstanding JWT for this user.
     * Called on password change, admin-forced revocation, suspected compromise, etc.
     */
    public void incrementTokenVersion() {
        tokenVersion++;
        updatedAt = Instant.now();
    }
}

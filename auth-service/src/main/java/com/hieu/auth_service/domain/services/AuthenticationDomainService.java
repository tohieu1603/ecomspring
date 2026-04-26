package com.hieu.auth_service.domain.services;

import com.hieu.auth_service.domain.models.permission.Permission;
import com.hieu.auth_service.domain.models.permission.vo.PermissionId;
import com.hieu.auth_service.domain.models.role.Role;
import com.hieu.auth_service.domain.models.user.User;
import com.hieu.auth_service.domain.models.user.exceptions.AccountNotUsableException;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cross-aggregate authorisation rules (permission/role checks) that span the
 * {@link User}, {@link Role}, and {@link Permission} aggregates.
 *
 * <p>Stateless, framework-free — can run inside any layer or test harness.
 */
public class AuthenticationDomainService {

    /** Does the user have a named permission through any of their roles? */
    public boolean hasPermission(User user, List<Role> userRoles,
                                 List<Permission> allSystemPermissions, String permissionName) {
        Set<String> grantedPermissionIds = extractPermissionIds(userRoles);
        return allSystemPermissions.stream()
                .filter(p -> grantedPermissionIds.contains(p.getId().value()))
                .anyMatch(p -> p.getName().equals(permissionName));
    }

    public boolean hasRole(User user, List<Role> userRoles, String roleName) {
        return userRoles.stream().anyMatch(r -> r.getName().equals(roleName));
    }

    /** Every permission name currently granted to the user. */
    public Set<String> getPermissionNames(User user, List<Role> userRoles,
                                          List<Permission> allSystemPermissions) {
        Set<String> grantedPermissionIds = extractPermissionIds(userRoles);
        return allSystemPermissions.stream()
                .filter(p -> grantedPermissionIds.contains(p.getId().value()))
                .map(p -> p.getName().value())
                .collect(Collectors.toSet());
    }

    /** Resource+action authorisation check. */
    public boolean canAccessResource(User user, List<Role> userRoles,
                                     List<Permission> allSystemPermissions,
                                     String resource, String action) {
        Set<String> grantedPermissionIds = extractPermissionIds(userRoles);
        return allSystemPermissions.stream()
                .filter(p -> grantedPermissionIds.contains(p.getId().value()))
                .anyMatch(p -> p.grants(resource, action));
    }

    /**
     * Validates account status at authentication time. Throws
     * {@link AccountNotUsableException} with a specific reason if the account is
     * disabled/locked/expired/credentials-expired.
     */
    public void validateAccountForAuthentication(User user) {
        if (user.isActive()) return;

        var s = user.getAccountStatus();
        if (!s.enabled())               throw new AccountNotUsableException(AccountNotUsableException.Reason.DISABLED);
        if (!s.accountNonLocked())      throw new AccountNotUsableException(AccountNotUsableException.Reason.LOCKED);
        if (!s.accountNonExpired())     throw new AccountNotUsableException(AccountNotUsableException.Reason.EXPIRED);
        if (!s.credentialsNonExpired()) throw new AccountNotUsableException(AccountNotUsableException.Reason.CREDENTIALS_EXPIRED);
    }

    private Set<String> extractPermissionIds(Collection<Role> roles) {
        return roles.stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(PermissionId::value)
                .collect(Collectors.toSet());
    }
}

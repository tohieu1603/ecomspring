package com.hieu.auth_service.application.mapper;

import com.hieu.auth_service.application.dto.PermissionDTO;
import com.hieu.auth_service.application.dto.RoleDTO;
import com.hieu.auth_service.application.dto.UserDTO;
import com.hieu.auth_service.domain.models.permission.Permission;
import com.hieu.auth_service.domain.models.role.Role;
import com.hieu.auth_service.domain.models.user.User;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Maps domain aggregates to read-model DTOs for the interface layer.
 *
 * <p>Kept in {@code application} (not domain) because DTOs are a use-case output
 * concern. Mapping here also insulates the domain from Jackson / framework concerns.
 */
@Component
public class UserDtoMapper {

    /**
     * Projects a {@link User} plus resolved role/permission collections into a {@link UserDTO}.
     *
     * @param user             source aggregate
     * @param roles            roles assigned to the user (already fetched)
     * @param effectivePermissions all permissions granted via roles (already resolved)
     * @return populated read model
     */
    public UserDTO toDto(User user, Collection<Role> roles, Collection<Permission> effectivePermissions) {
        Set<String> roleNames = roles.stream()
                .map(r -> r.getName().value())
                .collect(Collectors.toSet());
        Set<String> permissionNames = effectivePermissions.stream()
                .map(p -> p.getName().value())
                .collect(Collectors.toSet());

        var s = user.getAccountStatus();
        return new UserDTO(
                user.getId().value(),
                user.getUsername().value(),
                user.getEmail().value(),
                user.getPersonName().firstName(),
                user.getPersonName().lastName(),
                s.enabled(),
                s.accountNonExpired(),
                s.accountNonLocked(),
                s.credentialsNonExpired(),
                roleNames,
                permissionNames,
                user.getCreatedAt(),
                user.getUpdatedAt(),
                s.lastLogin()
        );
    }

    /** Overload that omits permissions (cheap projection when the caller doesn't need them). */
    public UserDTO toDto(User user, Collection<Role> roles) {
        return toDto(user, roles, List.of());
    }

    /**
     * Projects a role aggregate to its read model.
     *
     * @param role           source role
     * @param grantedPermissions permission names effectively granted (names preferred over ids)
     * @return populated read model
     */
    public RoleDTO toDto(Role role, Set<String> grantedPermissions) {
        return new RoleDTO(
                role.getId().value(),
                role.getName().value(),
                role.getDescription(),
                grantedPermissions,
                role.getCreatedAt(),
                role.getUpdatedAt()
        );
    }

    /** Projects a permission aggregate to its read model. */
    public PermissionDTO toDto(Permission p) {
        return new PermissionDTO(
                p.getId().value(),
                p.getName().value(),
                p.getName().resource(),
                p.getName().action(),
                p.getDescription(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}

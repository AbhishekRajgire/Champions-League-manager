package com.ucl.service;

import com.ucl.dto.RoleUpdateRequest;
import com.ucl.dto.UserResponse;
import com.ucl.exception.ApiException;
import com.ucl.model.Role;
import com.ucl.model.User;
import com.ucl.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Admin user administration: list accounts and change their roles. Guards
 * against an admin locking themselves out or removing the final admin.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuditService auditService;

    public UserService(UserRepository userRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getRole).thenComparing(User::getUsername))
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse updateRole(Long id, RoleUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + id));

        if (user.getUsername().equals(currentUsername())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot change your own role.");
        }

        Role oldRole = user.getRole();
        Role newRole = request.role();
        if (oldRole == newRole) {
            return UserResponse.from(user); // no-op
        }
        if (oldRole == Role.ADMIN && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Cannot change the role of the only remaining admin.");
        }

        user.setRole(newRole);
        UserResponse saved = UserResponse.from(userRepository.save(user));
        auditService.log("USER_ROLE_CHANGED", "User " + user.getUsername(), oldRole + " → " + newRole);
        return saved;
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }
}

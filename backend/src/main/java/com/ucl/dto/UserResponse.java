package com.ucl.dto;

import com.ucl.model.User;

public record UserResponse(
        Long id,
        String username,
        String role
) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getRole().name());
    }
}

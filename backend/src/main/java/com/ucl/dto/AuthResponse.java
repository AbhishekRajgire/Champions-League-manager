package com.ucl.dto;

public record AuthResponse(
        String token,
        String username,
        String role
) {
}

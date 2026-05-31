package com.ucl.dto;

public record GenerationResponse(
        boolean success,
        String message,
        int teams,
        int fixtures,
        int matchdays,
        int attempts
) {
}

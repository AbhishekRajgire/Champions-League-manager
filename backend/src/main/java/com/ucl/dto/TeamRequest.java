package com.ucl.dto;

import com.ucl.model.Pot;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TeamRequest(
        @NotBlank String name,
        @NotBlank String country,
        @NotNull Pot pot,
        String logoUrl
) {
}

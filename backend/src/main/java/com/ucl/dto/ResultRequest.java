package com.ucl.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ResultRequest(
        @NotNull @Min(0) Integer homeScore,
        @NotNull @Min(0) Integer awayScore
) {
}

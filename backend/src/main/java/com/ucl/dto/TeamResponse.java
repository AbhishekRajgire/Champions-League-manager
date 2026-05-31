package com.ucl.dto;

import com.ucl.model.Team;

public record TeamResponse(
        Long id,
        String name,
        String country,
        String pot,
        String logoUrl
) {
    public static TeamResponse from(Team t) {
        return new TeamResponse(t.getId(), t.getName(), t.getCountry(), t.getPot().name(), t.getLogoUrl());
    }
}

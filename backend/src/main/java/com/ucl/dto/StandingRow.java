package com.ucl.dto;

public record StandingRow(
        int position,
        Long teamId,
        String team,
        String country,
        String pot,
        int played,
        int won,
        int drawn,
        int lost,
        int goalsFor,
        int goalsAgainst,
        int goalDifference,
        int points
) {
}

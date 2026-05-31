package com.ucl.dto;

import com.ucl.model.Fixture;

public record FixtureResponse(
        Long id,
        int matchday,
        Long homeTeamId,
        String homeTeam,
        String homeCountry,
        String homePot,
        Long awayTeamId,
        String awayTeam,
        String awayCountry,
        String awayPot,
        Integer homeScore,
        Integer awayScore,
        boolean played
) {
    public static FixtureResponse from(Fixture f) {
        return new FixtureResponse(
                f.getId(),
                f.getMatchday(),
                f.getHomeTeam().getId(),
                f.getHomeTeam().getName(),
                f.getHomeTeam().getCountry(),
                f.getHomeTeam().getPot().name(),
                f.getAwayTeam().getId(),
                f.getAwayTeam().getName(),
                f.getAwayTeam().getCountry(),
                f.getAwayTeam().getPot().name(),
                f.getHomeScore(),
                f.getAwayScore(),
                f.isPlayed()
        );
    }
}

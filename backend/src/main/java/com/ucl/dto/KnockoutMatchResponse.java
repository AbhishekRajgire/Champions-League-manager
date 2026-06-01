package com.ucl.dto;

import com.ucl.model.KnockoutMatch;
import com.ucl.model.Team;

public record KnockoutMatchResponse(
        Long id,
        String round,
        int slot,
        Long homeTeamId,
        String homeTeam,
        Integer homeSeed,
        Long awayTeamId,
        String awayTeam,
        Integer awaySeed,
        Integer homeScore,
        Integer awayScore,
        boolean played,
        Long winnerTeamId,
        String winner
) {
    public static KnockoutMatchResponse from(KnockoutMatch m) {
        Team home = m.getHomeTeam();
        Team away = m.getAwayTeam();
        Team won = m.getWinner();
        return new KnockoutMatchResponse(
                m.getId(),
                m.getRound().name(),
                m.getSlot(),
                home == null ? null : home.getId(),
                home == null ? null : home.getName(),
                m.getHomeSeed(),
                away == null ? null : away.getId(),
                away == null ? null : away.getName(),
                m.getAwaySeed(),
                m.getHomeScore(),
                m.getAwayScore(),
                m.isPlayed(),
                won == null ? null : won.getId(),
                won == null ? null : won.getName()
        );
    }
}

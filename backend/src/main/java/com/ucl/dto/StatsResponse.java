package com.ucl.dto;

import java.util.List;

public record StatsResponse(
        int totalTeams,
        int totalFixtures,
        int playedFixtures,
        int remainingFixtures,
        int totalGoals,
        double averageGoalsPerMatch,
        int biggestWinMargin,
        String biggestWinScoreline,
        List<TeamGoalStat> topScoringTeams,
        List<TeamGoalStat> bestDefences
) {
    public record TeamGoalStat(String team, int value) {
    }
}

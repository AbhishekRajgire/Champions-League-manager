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
        int biggestHomeWinMargin,
        String biggestHomeWinScoreline,
        int biggestAwayWinMargin,
        String biggestAwayWinScoreline,
        TeamGoalStat mostWins,
        TeamGoalStat mostLosses,
        List<TeamGoalStat> topScoringTeams,
        List<TeamGoalStat> bestDefences,
        List<TeamGoalStat> mostCleanSheets,
        List<TeamForm> teamForms
) {
    public record TeamGoalStat(String team, int value) {
    }

    /**
     * Form summary for a single team.
     *
     * @param streakType   the kind of the current unbeaten/losing run: "W", "D", "L", or "-" if no matches played
     * @param streakLength how many consecutive matches the streak spans (0 if no matches played)
     * @param last5        the last (up to) 5 results in chronological order, oldest first, each "W"/"D"/"L"
     */
    public record TeamForm(String team, String streakType, int streakLength, List<String> last5) {
    }
}

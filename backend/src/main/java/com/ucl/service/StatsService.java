package com.ucl.service;

import com.ucl.dto.StatsResponse;
import com.ucl.model.Fixture;
import com.ucl.repository.FixtureRepository;
import com.ucl.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatsService {

    private final TeamRepository teamRepository;
    private final FixtureRepository fixtureRepository;

    public StatsService(TeamRepository teamRepository, FixtureRepository fixtureRepository) {
        this.teamRepository = teamRepository;
        this.fixtureRepository = fixtureRepository;
    }

    public StatsResponse getStats() {
        List<Fixture> all = fixtureRepository.findAll();
        int total = all.size();
        int played = 0;
        int totalGoals = 0;
        int biggestMargin = -1;
        String biggestScoreline = "-";

        Map<String, Integer> scoredBy = new HashMap<>();
        Map<String, Integer> concededBy = new HashMap<>();

        for (Fixture f : all) {
            if (!f.isPlayed() || f.getHomeScore() == null || f.getAwayScore() == null) {
                continue;
            }
            played++;
            int hs = f.getHomeScore();
            int as = f.getAwayScore();
            totalGoals += hs + as;

            String homeName = f.getHomeTeam().getName();
            String awayName = f.getAwayTeam().getName();
            scoredBy.merge(homeName, hs, Integer::sum);
            scoredBy.merge(awayName, as, Integer::sum);
            concededBy.merge(homeName, as, Integer::sum);
            concededBy.merge(awayName, hs, Integer::sum);

            int margin = Math.abs(hs - as);
            if (margin > biggestMargin) {
                biggestMargin = margin;
                biggestScoreline = homeName + " " + hs + "-" + as + " " + awayName;
            }
        }

        double avg = played == 0 ? 0.0 : Math.round((totalGoals / (double) played) * 100.0) / 100.0;

        List<StatsResponse.TeamGoalStat> topScorers = scoredBy.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(e -> new StatsResponse.TeamGoalStat(e.getKey(), e.getValue()))
                .toList();

        List<StatsResponse.TeamGoalStat> bestDefences = concededBy.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getValue))
                .limit(5)
                .map(e -> new StatsResponse.TeamGoalStat(e.getKey(), e.getValue()))
                .toList();

        return new StatsResponse(
                (int) teamRepository.count(),
                total,
                played,
                total - played,
                totalGoals,
                avg,
                Math.max(biggestMargin, 0),
                played == 0 ? "-" : biggestScoreline,
                topScorers,
                bestDefences);
    }
}

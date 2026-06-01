package com.ucl.service;

import com.ucl.dto.StatsResponse;
import com.ucl.model.Fixture;
import com.ucl.model.Team;
import com.ucl.repository.FixtureRepository;
import com.ucl.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatsService {

    private static final int FORM_WINDOW = 5;

    private final TeamRepository teamRepository;
    private final FixtureRepository fixtureRepository;

    public StatsService(TeamRepository teamRepository, FixtureRepository fixtureRepository) {
        this.teamRepository = teamRepository;
        this.fixtureRepository = fixtureRepository;
    }

    public StatsResponse getStats() {
        // Seed an accumulator for every team so teams that have not yet played still appear.
        Map<Long, Acc> table = new LinkedHashMap<>();
        for (Team t : teamRepository.findAll()) {
            table.put(t.getId(), new Acc(t.getName()));
        }

        // Walk fixtures in chronological order (matchday, then id) so streaks and form are correct.
        List<Fixture> all = fixtureRepository.findAllByOrderByMatchdayAscIdAsc();
        int total = all.size();
        int played = 0;
        int totalGoals = 0;

        int biggestMargin = -1;
        String biggestScoreline = "-";
        int biggestHomeMargin = -1;
        String biggestHomeScoreline = "-";
        int biggestAwayMargin = -1;
        String biggestAwayScoreline = "-";

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
            Acc home = table.get(f.getHomeTeam().getId());
            Acc away = table.get(f.getAwayTeam().getId());

            home.goalsFor += hs;
            home.goalsAgainst += as;
            away.goalsFor += as;
            away.goalsAgainst += hs;
            if (as == 0) {
                home.cleanSheets++;
            }
            if (hs == 0) {
                away.cleanSheets++;
            }

            if (hs > as) {
                home.wins++;
                away.losses++;
                home.results.add("W");
                away.results.add("L");
            } else if (hs < as) {
                away.wins++;
                home.losses++;
                home.results.add("L");
                away.results.add("W");
            } else {
                home.results.add("D");
                away.results.add("D");
            }

            int margin = Math.abs(hs - as);
            String scoreline = homeName + " " + hs + "-" + as + " " + awayName;
            if (margin > biggestMargin) {
                biggestMargin = margin;
                biggestScoreline = scoreline;
            }
            if (hs > as && (hs - as) > biggestHomeMargin) {
                biggestHomeMargin = hs - as;
                biggestHomeScoreline = scoreline;
            }
            if (as > hs && (as - hs) > biggestAwayMargin) {
                biggestAwayMargin = as - hs;
                biggestAwayScoreline = scoreline;
            }
        }

        double avg = played == 0 ? 0.0 : Math.round((totalGoals / (double) played) * 100.0) / 100.0;

        List<Acc> accs = new ArrayList<>(table.values());

        List<StatsResponse.TeamGoalStat> topScorers = accs.stream()
                .sorted(Comparator.comparingInt((Acc a) -> a.goalsFor).reversed())
                .limit(5)
                .map(a -> new StatsResponse.TeamGoalStat(a.name, a.goalsFor))
                .toList();

        List<StatsResponse.TeamGoalStat> bestDefences = accs.stream()
                .sorted(Comparator.comparingInt((Acc a) -> a.goalsAgainst))
                .limit(5)
                .map(a -> new StatsResponse.TeamGoalStat(a.name, a.goalsAgainst))
                .toList();

        List<StatsResponse.TeamGoalStat> mostCleanSheets = accs.stream()
                .sorted(Comparator.comparingInt((Acc a) -> a.cleanSheets).reversed())
                .limit(5)
                .map(a -> new StatsResponse.TeamGoalStat(a.name, a.cleanSheets))
                .toList();

        StatsResponse.TeamGoalStat mostWins = accs.stream()
                .max(Comparator.comparingInt((Acc a) -> a.wins))
                .filter(a -> a.wins > 0)
                .map(a -> new StatsResponse.TeamGoalStat(a.name, a.wins))
                .orElse(null);

        StatsResponse.TeamGoalStat mostLosses = accs.stream()
                .max(Comparator.comparingInt((Acc a) -> a.losses))
                .filter(a -> a.losses > 0)
                .map(a -> new StatsResponse.TeamGoalStat(a.name, a.losses))
                .orElse(null);

        List<StatsResponse.TeamForm> teamForms = accs.stream()
                .sorted(Comparator.comparing(a -> a.name))
                .map(Acc::toForm)
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
                Math.max(biggestHomeMargin, 0),
                biggestHomeScoreline,
                Math.max(biggestAwayMargin, 0),
                biggestAwayScoreline,
                mostWins,
                mostLosses,
                topScorers,
                bestDefences,
                mostCleanSheets,
                teamForms);
    }

    private static final class Acc {
        final String name;
        final List<String> results = new ArrayList<>(); // chronological W/D/L
        int goalsFor, goalsAgainst, wins, losses, cleanSheets;

        Acc(String name) {
            this.name = name;
        }

        StatsResponse.TeamForm toForm() {
            if (results.isEmpty()) {
                return new StatsResponse.TeamForm(name, "-", 0, List.of());
            }
            // Current streak: trailing run of identical results from the most recent match.
            String last = results.get(results.size() - 1);
            int length = 0;
            for (int i = results.size() - 1; i >= 0 && results.get(i).equals(last); i--) {
                length++;
            }
            List<String> last5 = results.subList(Math.max(0, results.size() - FORM_WINDOW), results.size());
            return new StatsResponse.TeamForm(name, last, length, List.copyOf(last5));
        }
    }
}

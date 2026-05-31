package com.ucl.service;

import com.ucl.dto.StandingRow;
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
public class StandingsService {

    private final TeamRepository teamRepository;
    private final FixtureRepository fixtureRepository;

    public StandingsService(TeamRepository teamRepository, FixtureRepository fixtureRepository) {
        this.teamRepository = teamRepository;
        this.fixtureRepository = fixtureRepository;
    }

    public List<StandingRow> getStandings() {
        Map<Long, Acc> table = new LinkedHashMap<>();
        for (Team t : teamRepository.findAll()) {
            table.put(t.getId(), new Acc(t));
        }

        for (Fixture f : fixtureRepository.findAll()) {
            if (!f.isPlayed() || f.getHomeScore() == null || f.getAwayScore() == null) {
                continue;
            }
            Acc home = table.get(f.getHomeTeam().getId());
            Acc away = table.get(f.getAwayTeam().getId());
            if (home == null || away == null) {
                continue;
            }
            int hs = f.getHomeScore();
            int as = f.getAwayScore();
            home.played++;
            away.played++;
            home.goalsFor += hs;
            home.goalsAgainst += as;
            away.goalsFor += as;
            away.goalsAgainst += hs;
            if (hs > as) {
                home.won++;
                home.points += 3;
                away.lost++;
            } else if (hs < as) {
                away.won++;
                away.points += 3;
                home.lost++;
            } else {
                home.drawn++;
                away.drawn++;
                home.points++;
                away.points++;
            }
        }

        List<Acc> rows = new ArrayList<>(table.values());
        rows.sort(Comparator
                .comparingInt((Acc a) -> a.points).reversed()
                .thenComparing(Comparator.comparingInt((Acc a) -> a.goalsFor - a.goalsAgainst).reversed())
                .thenComparing(Comparator.comparingInt((Acc a) -> a.goalsFor).reversed())
                .thenComparing(a -> a.team.getName()));

        List<StandingRow> result = new ArrayList<>(rows.size());
        int pos = 1;
        for (Acc a : rows) {
            result.add(new StandingRow(
                    pos++,
                    a.team.getId(),
                    a.team.getName(),
                    a.team.getCountry(),
                    a.team.getPot().name(),
                    a.played, a.won, a.drawn, a.lost,
                    a.goalsFor, a.goalsAgainst,
                    a.goalsFor - a.goalsAgainst,
                    a.points));
        }
        return result;
    }

    private static final class Acc {
        final Team team;
        int played, won, drawn, lost, goalsFor, goalsAgainst, points;

        Acc(Team team) {
            this.team = team;
        }
    }
}

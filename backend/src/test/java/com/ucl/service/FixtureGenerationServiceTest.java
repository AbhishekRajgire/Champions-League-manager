package com.ucl.service;

import com.ucl.dto.GenerationResponse;
import com.ucl.model.Fixture;
import com.ucl.model.Pot;
import com.ucl.model.Team;
import com.ucl.repository.FixtureRepository;
import com.ucl.repository.TeamRepository;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies that the generated schedule honours every Champions League draw rule.
 * Uses the real 2024/25 36-team line-up (the rules are known to be satisfiable for it).
 */
class FixtureGenerationServiceTest {

    private List<Team> sampleTeams() {
        List<Team> teams = new ArrayList<>();
        addPot(teams, Pot.POT1,
                t("Real Madrid", "Spain"), t("Manchester City", "England"), t("Bayern Munich", "Germany"),
                t("PSG", "France"), t("Liverpool", "England"), t("Inter", "Italy"),
                t("Dortmund", "Germany"), t("RB Leipzig", "Germany"), t("Barcelona", "Spain"));
        addPot(teams, Pot.POT2,
                t("Leverkusen", "Germany"), t("Atletico Madrid", "Spain"), t("Atalanta", "Italy"),
                t("Juventus", "Italy"), t("Benfica", "Portugal"), t("Arsenal", "England"),
                t("Club Brugge", "Belgium"), t("Shakhtar", "Ukraine"), t("AC Milan", "Italy"));
        addPot(teams, Pot.POT3,
                t("Feyenoord", "Netherlands"), t("Sporting", "Portugal"), t("PSV", "Netherlands"),
                t("Dinamo Zagreb", "Croatia"), t("Salzburg", "Austria"), t("Lille", "France"),
                t("Red Star", "Serbia"), t("Young Boys", "Switzerland"), t("Celtic", "Scotland"));
        addPot(teams, Pot.POT4,
                t("Slovan Bratislava", "Slovakia"), t("Monaco", "France"), t("Sparta Prague", "Czechia"),
                t("Aston Villa", "England"), t("Bologna", "Italy"), t("Girona", "Spain"),
                t("Stuttgart", "Germany"), t("Sturm Graz", "Austria"), t("Brest", "France"));
        long id = 1;
        for (Team team : teams) {
            team.setId(id++);
        }
        return teams;
    }

    private static Team t(String name, String country) {
        return new Team(name, country, Pot.POT1); // pot overwritten by addPot
    }

    private static void addPot(List<Team> list, Pot pot, Team... teams) {
        for (Team team : teams) {
            team.setPot(pot);
            list.add(team);
        }
    }

    @RepeatedTest(5) // run a few times since the generator is randomized
    void generatesScheduleSatisfyingAllRules() {
        List<Team> teams = sampleTeams();

        TeamRepository teamRepo = mock(TeamRepository.class);
        FixtureRepository fixtureRepo = mock(FixtureRepository.class);
        when(teamRepo.findAll()).thenReturn(teams);

        ArgumentCaptor<List<Fixture>> captor = ArgumentCaptor.forClass(List.class);
        FixtureGenerationService service = new FixtureGenerationService(teamRepo, fixtureRepo, 50000);

        GenerationResponse response = service.generate();
        assertThat(response.success()).isTrue();

        when(fixtureRepo.saveAll(anyList())).thenReturn(List.of());
        // capture what was passed to saveAll
        org.mockito.Mockito.verify(fixtureRepo).saveAll(captor.capture());
        List<Fixture> fixtures = captor.getValue();

        // 36 teams * 8 / 2 = 144 fixtures
        assertThat(fixtures).hasSize(144);

        // Per-team aggregates
        Map<Long, int[]> perPot = new HashMap<>();      // teamId -> count vs each pot
        Map<Long, Integer> homeCount = new HashMap<>();
        Map<Long, Integer> awayCount = new HashMap<>();
        Map<Long, int[]> homePerPot = new HashMap<>();   // home matches vs each pot
        Map<Long, int[]> awayPerPot = new HashMap<>();
        Map<Long, Map<String, Integer>> countryCount = new HashMap<>();
        Map<Long, Integer> totalMatches = new HashMap<>();

        for (Team team : teams) {
            perPot.put(team.getId(), new int[4]);
            homePerPot.put(team.getId(), new int[4]);
            awayPerPot.put(team.getId(), new int[4]);
            countryCount.put(team.getId(), new HashMap<>());
        }

        for (Fixture f : fixtures) {
            Team home = f.getHomeTeam();
            Team away = f.getAwayTeam();

            // never same country
            assertThat(home.getCountry())
                    .as("home and away must be different countries")
                    .isNotEqualToIgnoringCase(away.getCountry());

            totalMatches.merge(home.getId(), 1, Integer::sum);
            totalMatches.merge(away.getId(), 1, Integer::sum);
            homeCount.merge(home.getId(), 1, Integer::sum);
            awayCount.merge(away.getId(), 1, Integer::sum);

            perPot.get(home.getId())[away.getPot().ordinal()]++;
            perPot.get(away.getId())[home.getPot().ordinal()]++;
            homePerPot.get(home.getId())[away.getPot().ordinal()]++;
            awayPerPot.get(away.getId())[home.getPot().ordinal()]++;

            countryCount.get(home.getId()).merge(away.getCountry(), 1, Integer::sum);
            countryCount.get(away.getId()).merge(home.getCountry(), 1, Integer::sum);
        }

        for (Team team : teams) {
            long id = team.getId();
            assertThat(totalMatches.get(id)).as("8 matches for %s", team.getName()).isEqualTo(8);
            assertThat(homeCount.getOrDefault(id, 0)).as("4 home for %s", team.getName()).isEqualTo(4);
            assertThat(awayCount.getOrDefault(id, 0)).as("4 away for %s", team.getName()).isEqualTo(4);

            for (int p = 0; p < 4; p++) {
                assertThat(perPot.get(id)[p]).as("2 opponents from pot %d for %s", p + 1, team.getName()).isEqualTo(2);
                assertThat(homePerPot.get(id)[p]).as("1 home vs pot %d for %s", p + 1, team.getName()).isEqualTo(1);
                assertThat(awayPerPot.get(id)[p]).as("1 away vs pot %d for %s", p + 1, team.getName()).isEqualTo(1);
            }

            for (Map.Entry<String, Integer> e : countryCount.get(id).entrySet()) {
                assertThat(e.getValue())
                        .as("%s faces at most 2 clubs from %s", team.getName(), e.getKey())
                        .isLessThanOrEqualTo(2);
            }
        }
    }
}

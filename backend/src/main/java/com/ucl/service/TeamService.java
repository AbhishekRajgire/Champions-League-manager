package com.ucl.service;

import com.ucl.dto.TeamRequest;
import com.ucl.dto.TeamResponse;
import com.ucl.exception.ApiException;
import com.ucl.model.Pot;
import com.ucl.model.Team;
import com.ucl.repository.FixtureRepository;
import com.ucl.repository.TeamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final FixtureRepository fixtureRepository;

    public TeamService(TeamRepository teamRepository, FixtureRepository fixtureRepository) {
        this.teamRepository = teamRepository;
        this.fixtureRepository = fixtureRepository;
    }

    public List<TeamResponse> getAllTeams() {
        return teamRepository.findAll().stream()
                .sorted(Comparator.comparing(Team::getPot).thenComparing(Team::getName))
                .map(TeamResponse::from)
                .toList();
    }

    @Transactional
    public TeamResponse createTeam(TeamRequest request) {
        if (teamRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new ApiException(HttpStatus.CONFLICT, "A team named '" + request.name() + "' already exists");
        }
        Team team = new Team(request.name().trim(), request.country().trim(), request.pot());
        team.setLogoUrl(request.logoUrl());
        return TeamResponse.from(teamRepository.save(team));
    }

    @Transactional
    public TeamResponse updateTeam(Long id, TeamRequest request) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Team not found: " + id));

        if (!team.getName().equalsIgnoreCase(request.name().trim())
                && teamRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new ApiException(HttpStatus.CONFLICT, "A team named '" + request.name() + "' already exists");
        }
        team.setName(request.name().trim());
        team.setCountry(request.country().trim());
        team.setPot(request.pot());
        team.setLogoUrl(request.logoUrl());
        return TeamResponse.from(teamRepository.save(team));
    }

    @Transactional
    public void deleteTeam(Long id) {
        if (!teamRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Team not found: " + id);
        }
        if (fixtureRepository.count() > 0) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Cannot delete a team while fixtures exist. Reset fixtures first.");
        }
        teamRepository.deleteById(id);
    }

    /**
     * Wipes all teams and fixtures, then loads the real 36-team UCL 2024/25
     * league-phase line-up (9 teams per pot). Handy for demos/testing the generator.
     */
    @Transactional
    public List<TeamResponse> loadSampleTeams() {
        fixtureRepository.deleteAllInBatch();
        teamRepository.deleteAllInBatch();

        List<Team> sample = new ArrayList<>();
        // Pot 1
        sample.add(new Team("Real Madrid", "Spain", Pot.POT1));
        sample.add(new Team("Manchester City", "England", Pot.POT1));
        sample.add(new Team("Bayern Munich", "Germany", Pot.POT1));
        sample.add(new Team("Paris Saint-Germain", "France", Pot.POT1));
        sample.add(new Team("Liverpool", "England", Pot.POT1));
        sample.add(new Team("Inter Milan", "Italy", Pot.POT1));
        sample.add(new Team("Borussia Dortmund", "Germany", Pot.POT1));
        sample.add(new Team("RB Leipzig", "Germany", Pot.POT1));
        sample.add(new Team("Barcelona", "Spain", Pot.POT1));
        // Pot 2
        sample.add(new Team("Bayer Leverkusen", "Germany", Pot.POT2));
        sample.add(new Team("Atletico Madrid", "Spain", Pot.POT2));
        sample.add(new Team("Atalanta", "Italy", Pot.POT2));
        sample.add(new Team("Juventus", "Italy", Pot.POT2));
        sample.add(new Team("Benfica", "Portugal", Pot.POT2));
        sample.add(new Team("Arsenal", "England", Pot.POT2));
        sample.add(new Team("Club Brugge", "Belgium", Pot.POT2));
        sample.add(new Team("Shakhtar Donetsk", "Ukraine", Pot.POT2));
        sample.add(new Team("AC Milan", "Italy", Pot.POT2));
        // Pot 3
        sample.add(new Team("Feyenoord", "Netherlands", Pot.POT3));
        sample.add(new Team("Sporting CP", "Portugal", Pot.POT3));
        sample.add(new Team("PSV Eindhoven", "Netherlands", Pot.POT3));
        sample.add(new Team("Dinamo Zagreb", "Croatia", Pot.POT3));
        sample.add(new Team("Red Bull Salzburg", "Austria", Pot.POT3));
        sample.add(new Team("Lille", "France", Pot.POT3));
        sample.add(new Team("Red Star Belgrade", "Serbia", Pot.POT3));
        sample.add(new Team("Young Boys", "Switzerland", Pot.POT3));
        sample.add(new Team("Celtic", "Scotland", Pot.POT3));
        // Pot 4
        sample.add(new Team("Slovan Bratislava", "Slovakia", Pot.POT4));
        sample.add(new Team("AS Monaco", "France", Pot.POT4));
        sample.add(new Team("Sparta Prague", "Czechia", Pot.POT4));
        sample.add(new Team("Aston Villa", "England", Pot.POT4));
        sample.add(new Team("Bologna", "Italy", Pot.POT4));
        sample.add(new Team("Girona", "Spain", Pot.POT4));
        sample.add(new Team("VfB Stuttgart", "Germany", Pot.POT4));
        sample.add(new Team("Sturm Graz", "Austria", Pot.POT4));
        sample.add(new Team("Stade Brestois", "France", Pot.POT4));

        teamRepository.saveAll(sample);
        return getAllTeams();
    }
}

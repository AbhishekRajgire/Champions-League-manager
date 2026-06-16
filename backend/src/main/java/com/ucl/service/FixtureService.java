package com.ucl.service;

import com.ucl.dto.FixtureResponse;
import com.ucl.dto.ResultRequest;
import com.ucl.exception.ApiException;
import com.ucl.model.Fixture;
import com.ucl.repository.FixtureRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FixtureService {

    private final FixtureRepository fixtureRepository;
    private final AuditService auditService;

    public FixtureService(FixtureRepository fixtureRepository, AuditService auditService) {
        this.fixtureRepository = fixtureRepository;
        this.auditService = auditService;
    }

    /** "MD3 · Real Madrid vs Barcelona" — a human label for the audit trail. */
    private static String label(Fixture f) {
        return "MD" + f.getMatchday() + " · " + f.getHomeTeam().getName() + " vs " + f.getAwayTeam().getName();
    }

    private static String scoreOf(Fixture f) {
        return f.isPlayed() && f.getHomeScore() != null && f.getAwayScore() != null
                ? f.getHomeScore() + "-" + f.getAwayScore()
                : "—"; // em dash = "not played"
    }

    public List<FixtureResponse> getAllFixtures() {
        return fixtureRepository.findAllByOrderByMatchdayAscIdAsc().stream()
                .map(FixtureResponse::from)
                .toList();
    }

    /** Fixtures grouped by matchday, for tabbed display in the UI. */
    public Map<Integer, List<FixtureResponse>> getFixturesByMatchday() {
        Map<Integer, List<FixtureResponse>> grouped = new LinkedHashMap<>();
        for (Fixture f : fixtureRepository.findAllByOrderByMatchdayAscIdAsc()) {
            grouped.computeIfAbsent(f.getMatchday(), k -> new java.util.ArrayList<>())
                    .add(FixtureResponse.from(f));
        }
        return grouped;
    }

    @Transactional
    public FixtureResponse updateResult(Long id, ResultRequest request) {
        Fixture fixture = fixtureRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Fixture not found: " + id));
        String before = scoreOf(fixture);
        fixture.setHomeScore(request.homeScore());
        fixture.setAwayScore(request.awayScore());
        fixture.setPlayed(true);
        FixtureResponse saved = FixtureResponse.from(fixtureRepository.save(fixture));
        auditService.log("RESULT_SET", label(fixture), before + " → " + scoreOf(fixture));
        return saved;
    }

    /** Clear a result back to "not played". */
    @Transactional
    public FixtureResponse clearResult(Long id) {
        Fixture fixture = fixtureRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Fixture not found: " + id));
        String before = scoreOf(fixture);
        fixture.setHomeScore(null);
        fixture.setAwayScore(null);
        fixture.setPlayed(false);
        FixtureResponse saved = FixtureResponse.from(fixtureRepository.save(fixture));
        auditService.log("RESULT_CLEARED", label(fixture), before + " → —");
        return saved;
    }

    @Transactional
    public void resetAllFixtures() {
        fixtureRepository.deleteAllInBatch();
    }
}

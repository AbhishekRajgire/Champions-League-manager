package com.ucl.service;

import com.ucl.dto.BracketResponse;
import com.ucl.dto.KnockoutMatchResponse;
import com.ucl.dto.ResultRequest;
import com.ucl.dto.StandingRow;
import com.ucl.exception.ApiException;
import com.ucl.model.KnockoutMatch;
import com.ucl.model.KnockoutRound;
import com.ucl.model.Team;
import com.ucl.repository.KnockoutMatchRepository;
import com.ucl.repository.TeamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Builds and drives the single-elimination knockout phase.
 *
 * <p>The top N teams from the league-phase standings qualify and are seeded
 * 1..N. The first round pairs them 1 v N, 2 v N-1, … with the higher seed at
 * home. Every later round is created empty up-front so the bracket exists as a
 * tree from the start; winners are slotted in as results are submitted. The
 * winner of {@code slot} advances to {@code slot / 2} of the next round (home
 * when {@code slot} is even, away when odd) and the loser is eliminated.
 */
@Service
public class KnockoutService {

    private final KnockoutMatchRepository knockoutMatchRepository;
    private final TeamRepository teamRepository;
    private final StandingsService standingsService;
    private final AuditService auditService;

    public KnockoutService(KnockoutMatchRepository knockoutMatchRepository,
                           TeamRepository teamRepository,
                           StandingsService standingsService,
                           AuditService auditService) {
        this.knockoutMatchRepository = knockoutMatchRepository;
        this.teamRepository = teamRepository;
        this.standingsService = standingsService;
        this.auditService = auditService;
    }

    /**
     * Seed the bracket from the current standings. {@code qualifiers} must be a
     * power of two between 2 and 16 (16 = start at the Round of 16). Any existing
     * bracket is discarded.
     */
    @Transactional
    public BracketResponse generate(int qualifiers) {
        KnockoutRound startRound = roundForQualifiers(qualifiers);

        List<StandingRow> standings = standingsService.getStandings();
        if (standings.size() < qualifiers) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Not enough teams to qualify: need " + qualifiers + " but only "
                            + standings.size() + " are in the standings.");
        }

        // Resolve the top N teams in seed order (index 0 = seed 1).
        List<Team> seeds = new ArrayList<>(qualifiers);
        for (int i = 0; i < qualifiers; i++) {
            Long teamId = standings.get(i).teamId();
            Team team = teamRepository.findById(teamId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Team not found: " + teamId));
            seeds.add(team);
        }

        knockoutMatchRepository.deleteAllInBatch();

        List<KnockoutMatch> matches = new ArrayList<>();

        // First round: pair seed (k+1) v seed (N-k), higher seed at home.
        for (int slot = 0; slot < startRound.getMatchCount(); slot++) {
            KnockoutMatch match = new KnockoutMatch(startRound, slot);
            int homeSeed = slot + 1;
            int awaySeed = qualifiers - slot;
            match.setHomeTeam(seeds.get(homeSeed - 1));
            match.setHomeSeed(homeSeed);
            match.setAwayTeam(seeds.get(awaySeed - 1));
            match.setAwaySeed(awaySeed);
            matches.add(match);
        }

        // Later rounds: created empty, to be filled as winners advance.
        for (KnockoutRound round = startRound.next(); round != null; round = round.next()) {
            for (int slot = 0; slot < round.getMatchCount(); slot++) {
                matches.add(new KnockoutMatch(round, slot));
            }
        }

        knockoutMatchRepository.saveAll(matches);
        return getBracket();
    }

    /** Record a result, decide the winner and advance them to the next round. */
    @Transactional
    public KnockoutMatchResponse submitResult(Long id, ResultRequest request) {
        KnockoutMatch match = knockoutMatchRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Knockout match not found: " + id));

        if (match.getHomeTeam() == null || match.getAwayTeam() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Both teams must be decided before this match can be played.");
        }
        if (request.homeScore().equals(request.awayScore())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Knockout matches cannot end level — enter a result with a winner.");
        }

        String before = match.isPlayed() && match.getHomeScore() != null
                ? match.getHomeScore() + "-" + match.getAwayScore() : "—";

        match.setHomeScore(request.homeScore());
        match.setAwayScore(request.awayScore());
        match.setPlayed(true);
        match.setWinner(request.homeScore() > request.awayScore() ? match.getHomeTeam() : match.getAwayTeam());
        knockoutMatchRepository.save(match);

        advanceWinner(match);

        String target = match.getRound().getLabel() + " · "
                + match.getHomeTeam().getName() + " vs " + match.getAwayTeam().getName();
        auditService.log("KNOCKOUT_RESULT", target,
                before + " → " + request.homeScore() + "-" + request.awayScore()
                        + " (" + match.getWinner().getName() + " advance)");
        return KnockoutMatchResponse.from(match);
    }

    /** The full bracket as a round → matches → teams tree. */
    public BracketResponse getBracket() {
        List<KnockoutMatch> all = knockoutMatchRepository.findAll();
        all.sort(Comparator
                .comparingInt((KnockoutMatch m) -> m.getRound().ordinal())
                .thenComparingInt(KnockoutMatch::getSlot));

        Map<KnockoutRound, List<KnockoutMatchResponse>> byRound = new EnumMap<>(KnockoutRound.class);
        for (KnockoutMatch m : all) {
            byRound.computeIfAbsent(m.getRound(), k -> new ArrayList<>()).add(KnockoutMatchResponse.from(m));
        }

        List<BracketResponse.RoundNode> rounds = new ArrayList<>();
        for (KnockoutRound round : KnockoutRound.values()) {
            List<KnockoutMatchResponse> matches = byRound.get(round);
            if (matches != null) {
                rounds.add(new BracketResponse.RoundNode(round.name(), round.getLabel(), matches));
            }
        }

        String champion = knockoutMatchRepository.findByRoundAndSlot(KnockoutRound.FINAL, 0)
                .filter(KnockoutMatch::isPlayed)
                .map(m -> m.getWinner().getName())
                .orElse(null);

        return new BracketResponse(champion, rounds);
    }

    @Transactional
    public void reset() {
        knockoutMatchRepository.deleteAllInBatch();
    }

    // ============================================================
    //  Internals
    // ============================================================

    /** Place a match's winner into its feeder slot of the next round. */
    private void advanceWinner(KnockoutMatch match) {
        KnockoutRound next = match.getRound().next();
        if (next == null) {
            return; // final — the winner is the champion
        }

        int nextSlot = match.getSlot() / 2;
        boolean asHome = match.getSlot() % 2 == 0;
        KnockoutMatch nextMatch = knockoutMatchRepository.findByRoundAndSlot(next, nextSlot)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT,
                        "Bracket is incomplete: missing " + next + " slot " + nextSlot + "."));

        Team winner = match.getWinner();
        Integer winnerSeed = winner.getId().equals(match.getHomeTeam().getId())
                ? match.getHomeSeed() : match.getAwaySeed();

        Team current = asHome ? nextMatch.getHomeTeam() : nextMatch.getAwayTeam();
        boolean changed = current == null || !current.getId().equals(winner.getId());

        if (asHome) {
            nextMatch.setHomeTeam(winner);
            nextMatch.setHomeSeed(winnerSeed);
        } else {
            nextMatch.setAwayTeam(winner);
            nextMatch.setAwaySeed(winnerSeed);
        }

        // If a previously decided result was overturned, the downstream match is
        // now stale — clear it (and anything it had fed forward) before saving.
        if (changed && nextMatch.isPlayed()) {
            clearResultCascade(nextMatch);
        }
        knockoutMatchRepository.save(nextMatch);
    }

    /** Reset a match's result and recursively undo the team it had advanced. */
    private void clearResultCascade(KnockoutMatch match) {
        Team formerWinner = match.getWinner();
        match.setHomeScore(null);
        match.setAwayScore(null);
        match.setPlayed(false);
        match.setWinner(null);
        knockoutMatchRepository.save(match);

        KnockoutRound next = match.getRound().next();
        if (next == null || formerWinner == null) {
            return;
        }
        int nextSlot = match.getSlot() / 2;
        boolean asHome = match.getSlot() % 2 == 0;
        knockoutMatchRepository.findByRoundAndSlot(next, nextSlot).ifPresent(nextMatch -> {
            if (asHome) {
                nextMatch.setHomeTeam(null);
                nextMatch.setHomeSeed(null);
            } else {
                nextMatch.setAwayTeam(null);
                nextMatch.setAwaySeed(null);
            }
            if (nextMatch.isPlayed()) {
                clearResultCascade(nextMatch);
            } else {
                knockoutMatchRepository.save(nextMatch);
            }
        });
    }

    private KnockoutRound roundForQualifiers(int qualifiers) {
        if (qualifiers < 2 || qualifiers > 16 || Integer.bitCount(qualifiers) != 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Number of qualifiers must be a power of two between 2 and 16 (got " + qualifiers + ").");
        }
        return KnockoutRound.forTeamCount(qualifiers);
    }
}

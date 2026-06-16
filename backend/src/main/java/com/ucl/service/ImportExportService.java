package com.ucl.service;

import com.ucl.dto.BracketResponse;
import com.ucl.dto.ImportResult;
import com.ucl.dto.KnockoutMatchResponse;
import com.ucl.dto.StandingRow;
import com.ucl.exception.ApiException;
import com.ucl.model.Fixture;
import com.ucl.model.Pot;
import com.ucl.model.Team;
import com.ucl.repository.FixtureRepository;
import com.ucl.repository.TeamRepository;
import com.ucl.util.CsvUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CSV import (teams, fixture results) and export (standings, knockout bracket).
 *
 * <p>Imports are tolerant: each row is validated independently and problems are
 * collected into the {@link ImportResult} rather than failing the whole upload.
 */
@Service
public class ImportExportService {

    private final TeamRepository teamRepository;
    private final FixtureRepository fixtureRepository;
    private final StandingsService standingsService;
    private final KnockoutService knockoutService;
    private final AuditService auditService;

    public ImportExportService(TeamRepository teamRepository,
                               FixtureRepository fixtureRepository,
                               StandingsService standingsService,
                               KnockoutService knockoutService,
                               AuditService auditService) {
        this.teamRepository = teamRepository;
        this.fixtureRepository = fixtureRepository;
        this.standingsService = standingsService;
        this.knockoutService = knockoutService;
        this.auditService = auditService;
    }

    // ============================================================
    //  Import
    // ============================================================

    /** CSV columns: name, country, pot[, logoUrl]. Existing teams are skipped. */
    @Transactional
    public ImportResult importTeams(MultipartFile file) {
        List<String[]> rows = readCsv(file);
        List<String> messages = new ArrayList<>();
        int succeeded = 0, skipped = 0, dataRows = 0;

        for (String[] row : rows) {
            if (isHeader(row, "name", "team")) {
                continue;
            }
            dataRows++;
            int lineNo = dataRows;
            try {
                if (row.length < 3) {
                    throw new IllegalArgumentException("expected at least name, country, pot");
                }
                String name = required(row[0], "name");
                String country = required(row[1], "country");
                Pot pot = parsePot(row[2]);
                String logoUrl = row.length > 3 ? blankToNull(row[3]) : null;

                if (teamRepository.existsByNameIgnoreCase(name)) {
                    skipped++;
                    messages.add("Row " + lineNo + ": skipped — '" + name + "' already exists");
                    continue;
                }
                Team team = new Team(name, country, pot);
                team.setLogoUrl(logoUrl);
                teamRepository.save(team);
                succeeded++;
            } catch (Exception ex) {
                skipped++;
                messages.add("Row " + lineNo + ": " + ex.getMessage());
            }
        }

        auditService.log("IMPORT_TEAMS", "Teams CSV",
                succeeded + " added, " + skipped + " skipped");
        return new ImportResult(dataRows, succeeded, skipped, messages);
    }

    /** CSV columns: matchday, homeTeam, awayTeam, homeScore, awayScore. */
    @Transactional
    public ImportResult importResults(MultipartFile file) {
        List<String[]> rows = readCsv(file);
        List<String> messages = new ArrayList<>();
        int succeeded = 0, skipped = 0, dataRows = 0;

        // Index fixtures by "matchday|home|away" (names lower-cased) for fast lookup.
        Map<String, Fixture> index = new HashMap<>();
        for (Fixture f : fixtureRepository.findAll()) {
            index.put(fixtureKey(f.getMatchday(), f.getHomeTeam().getName(), f.getAwayTeam().getName()), f);
        }

        for (String[] row : rows) {
            if (isHeader(row, "matchday", "md")) {
                continue;
            }
            dataRows++;
            int lineNo = dataRows;
            try {
                if (row.length < 5) {
                    throw new IllegalArgumentException("expected matchday, homeTeam, awayTeam, homeScore, awayScore");
                }
                int matchday = parseInt(row[0], "matchday");
                String home = required(row[1], "homeTeam");
                String away = required(row[2], "awayTeam");
                int hs = parseScore(row[3], "homeScore");
                int as = parseScore(row[4], "awayScore");

                Fixture fixture = index.get(fixtureKey(matchday, home, away));
                if (fixture == null) {
                    skipped++;
                    messages.add("Row " + lineNo + ": no fixture for MD" + matchday + " " + home + " vs " + away);
                    continue;
                }

                String before = fixture.isPlayed() && fixture.getHomeScore() != null
                        ? fixture.getHomeScore() + "-" + fixture.getAwayScore() : "—";
                fixture.setHomeScore(hs);
                fixture.setAwayScore(as);
                fixture.setPlayed(true);
                fixtureRepository.save(fixture);
                succeeded++;
                auditService.log("IMPORT_RESULT",
                        "MD" + matchday + " · " + fixture.getHomeTeam().getName()
                                + " vs " + fixture.getAwayTeam().getName(),
                        before + " → " + hs + "-" + as);
            } catch (Exception ex) {
                skipped++;
                messages.add("Row " + lineNo + ": " + ex.getMessage());
            }
        }
        return new ImportResult(dataRows, succeeded, skipped, messages);
    }

    // ============================================================
    //  Export
    // ============================================================

    public String exportStandingsCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append(CsvUtil.line("Position", "Team", "Country", "Pot",
                "Played", "Won", "Drawn", "Lost", "GoalsFor", "GoalsAgainst", "GoalDifference", "Points"));
        for (StandingRow r : standingsService.getStandings()) {
            sb.append(CsvUtil.line(r.position(), r.team(), r.country(), r.pot(),
                    r.played(), r.won(), r.drawn(), r.lost(),
                    r.goalsFor(), r.goalsAgainst(), r.goalDifference(), r.points()));
        }
        return sb.toString();
    }

    public String exportBracketCsv() {
        BracketResponse bracket = knockoutService.getBracket();
        StringBuilder sb = new StringBuilder();
        sb.append(CsvUtil.line("Round", "Slot", "HomeTeam", "HomeSeed",
                "AwayTeam", "AwaySeed", "HomeScore", "AwayScore", "Played", "Winner"));
        for (BracketResponse.RoundNode round : bracket.rounds()) {
            for (KnockoutMatchResponse m : round.matches()) {
                sb.append(CsvUtil.line(
                        round.label(), m.slot(),
                        orDash(m.homeTeam()), orBlank(m.homeSeed()),
                        orDash(m.awayTeam()), orBlank(m.awaySeed()),
                        orBlank(m.homeScore()), orBlank(m.awayScore()),
                        m.played(), orDash(m.winner())));
            }
        }
        if (bracket.champion() != null) {
            sb.append(CsvUtil.line("CHAMPION", "", bracket.champion(), "", "", "", "", "", "", ""));
        }
        return sb.toString();
    }

    // ============================================================
    //  Helpers
    // ============================================================

    private List<String[]> readCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No file uploaded, or the file is empty.");
        }
        try {
            List<String[]> rows = CsvUtil.parse(new String(file.getBytes(), StandardCharsets.UTF_8));
            if (rows.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "The CSV file has no rows.");
            }
            return rows;
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not read the uploaded file.");
        }
    }

    private static boolean isHeader(String[] row, String... firstCellOptions) {
        if (row.length == 0) {
            return false;
        }
        String first = row[0].trim().toLowerCase(Locale.ROOT);
        for (String opt : firstCellOptions) {
            if (first.equals(opt)) {
                return true;
            }
        }
        return false;
    }

    private static String fixtureKey(int matchday, String home, String away) {
        return matchday + "|" + home.trim().toLowerCase(Locale.ROOT) + "|" + away.trim().toLowerCase(Locale.ROOT);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private static int parseInt(String value, String field) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(field + " '" + value + "' is not a whole number");
        }
    }

    private static int parseScore(String value, String field) {
        int n = parseInt(value, field);
        if (n < 0) {
            throw new IllegalArgumentException(field + " cannot be negative");
        }
        return n;
    }

    /** Accepts POT1..POT4, "Pot 1", or a bare "1".."4". */
    private static Pot parsePot(String raw) {
        String v = raw.trim().toUpperCase(Locale.ROOT).replace(" ", "");
        switch (v) {
            case "1": case "POT1": return Pot.POT1;
            case "2": case "POT2": return Pot.POT2;
            case "3": case "POT3": return Pot.POT3;
            case "4": case "POT4": return Pot.POT4;
            default:
                throw new IllegalArgumentException("pot '" + raw + "' must be one of POT1..POT4 (or 1..4)");
        }
    }

    private static String orDash(String s) {
        return s == null ? "-" : s;
    }

    private static String orBlank(Object o) {
        return o == null ? "" : o.toString();
    }
}

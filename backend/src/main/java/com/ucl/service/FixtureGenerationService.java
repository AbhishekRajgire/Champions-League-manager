package com.ucl.service;

import com.ucl.dto.GenerationResponse;
import com.ucl.exception.ApiException;
import com.ucl.model.Fixture;
import com.ucl.model.Pot;
import com.ucl.model.Team;
import com.ucl.repository.FixtureRepository;
import com.ucl.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Generates a Champions-League "Swiss model" league-phase schedule.
 *
 * <p>Rules enforced:
 * <ul>
 *   <li>Teams are split across 4 pots.</li>
 *   <li>Each team plays exactly 2 opponents from every pot — one at home, one away.
 *       (=> 8 matches, 4 home + 4 away per team.)</li>
 *   <li>A team can never face another club from its own country.</li>
 *   <li>A team faces at most 2 clubs from any single country.</li>
 * </ul>
 *
 * <p>The opponent graph is built with randomized backtracking (fail-first / MRV
 * heuristic). Because the constraints can create dead-ends, the build is retried
 * until a valid configuration is found or {@code app.fixtures.max-attempts} is hit.
 * Home/away is then assigned by orienting each pot-pair's 2-regular graph as
 * directed cycles, guaranteeing exactly one home and one away match per pot.
 */
@Service
public class FixtureGenerationService {

    private static final int POTS = 4;
    private static final int OPPONENTS_PER_POT = 2;
    private static final int MAX_PER_COUNTRY = 2;

    private final TeamRepository teamRepository;
    private final FixtureRepository fixtureRepository;
    private final int maxAttempts;

    public FixtureGenerationService(TeamRepository teamRepository,
                                    FixtureRepository fixtureRepository,
                                    @Value("${app.fixtures.max-attempts:20000}") int maxAttempts) {
        this.teamRepository = teamRepository;
        this.fixtureRepository = fixtureRepository;
        this.maxAttempts = maxAttempts;
    }

    @Transactional
    public GenerationResponse generate() {
        List<Team> teams = teamRepository.findAll();
        validate(teams);

        // Stable indexing
        int n = teams.size();
        int[] potOf = new int[n];
        String[] countryOf = new String[n];
        for (int i = 0; i < n; i++) {
            potOf[i] = teams.get(i).getPot().ordinal();
            countryOf[i] = teams.get(i).getCountry().trim().toLowerCase(Locale.ROOT);
        }

        // ---- 1. Build the undirected opponent graph via randomized backtracking ----
        Random random = new Random();
        Set<Long> edges = null;          // packed (min*n + max)
        int usedAttempts = 0;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            usedAttempts = attempt;
            Builder builder = new Builder(n, potOf, countryOf, random);
            if (builder.build()) {
                edges = builder.edges;
                break;
            }
        }
        if (edges == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Could not generate a valid schedule after " + maxAttempts + " attempts. " +
                    "The registered teams may not satisfy the country constraints (e.g. too many clubs " +
                    "from one country, or not enough variety). Adjust the pots/countries and try again.");
        }

        // ---- 2. Orient home/away per pot-pair (directed cycles) ----
        List<int[]> directed = orient(edges, n, potOf, random); // each = {homeIdx, awayIdx}

        // ---- 3. Assign matchdays (split the schedule into GAMES_PER_TEAM balanced rounds) ----
        int gamesPerTeam = OPPONENTS_PER_POT * POTS; // each team plays this many matches (= matchdays)
        int matchdays = assignMatchdays(directed, n, gamesPerTeam, random);

        // ---- 4. Persist ----
        fixtureRepository.deleteAllInBatch();
        List<Fixture> fixtures = new ArrayList<>(directed.size());
        for (int[] d : directed) {
            fixtures.add(new Fixture(teams.get(d[0]), teams.get(d[1]), d[2]));
        }
        fixtureRepository.saveAll(fixtures);

        return new GenerationResponse(true,
                "Fixtures generated successfully.",
                n, fixtures.size(), matchdays, usedAttempts);
    }

    private void validate(List<Team> teams) {
        if (teams.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No teams registered yet.");
        }
        long[] potCounts = new long[POTS];
        for (Team t : teams) {
            potCounts[t.getPot().ordinal()]++;
        }
        for (Pot p : Pot.values()) {
            if (potCounts[p.ordinal()] == 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Pot " + (p.ordinal() + 1) + " has no teams. All four pots must be filled.");
            }
        }
        long size = potCounts[0];
        for (int p = 1; p < POTS; p++) {
            if (potCounts[p] != size) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "All four pots must contain the same number of teams. Current sizes: " +
                        Arrays.toString(potCounts) + ".");
            }
        }
        if (size < 3) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Each pot needs at least 3 teams (so a team can play 2 opponents from its own pot). " +
                    "Current pot size: " + size + ".");
        }
    }

    // ============================================================
    //  Backtracking builder for the undirected opponent graph
    // ============================================================
    private static final class Builder {
        final int n;
        final int[] potOf;
        final String[] countryOf;
        final Random random;

        final int[][] demand;                 // demand[team][pot] remaining opponents needed
        final List<Set<Integer>> opponents;   // chosen opponents per team
        final List<Map<String, Integer>> countryCount; // opponents' country tally per team
        final Set<Long> edges = new HashSet<>();
        int steps = 0;
        final int stepBudget;

        Builder(int n, int[] potOf, String[] countryOf, Random random) {
            this.n = n;
            this.potOf = potOf;
            this.countryOf = countryOf;
            this.random = random;
            this.demand = new int[n][POTS];
            this.opponents = new ArrayList<>(n);
            this.countryCount = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                Arrays.fill(demand[i], OPPONENTS_PER_POT);
                opponents.add(new HashSet<>());
                countryCount.add(new HashMap<>());
            }
            // Generous per-attempt budget; if exceeded we abandon and let caller restart.
            this.stepBudget = 200 * n;
        }

        boolean build() {
            return solve();
        }

        private boolean solve() {
            if (++steps > stepBudget) {
                return false;
            }
            // Pick the most-constrained open (team, pot) cell: fewest candidates (MRV / fail-first).
            int bestTeam = -1, bestPot = -1;
            List<Integer> bestCandidates = null;
            for (int i = 0; i < n; i++) {
                for (int p = 0; p < POTS; p++) {
                    if (demand[i][p] <= 0) continue;
                    List<Integer> candidates = candidatesFor(i, p);
                    if (candidates.isEmpty()) {
                        return false; // dead-end
                    }
                    if (bestCandidates == null || candidates.size() < bestCandidates.size()) {
                        bestCandidates = candidates;
                        bestTeam = i;
                        bestPot = p;
                        if (candidates.size() == 1) {
                            p = POTS; i = n; // forced move, stop scanning
                        }
                    }
                }
            }
            if (bestCandidates == null) {
                return true; // all demands satisfied -> solved
            }

            Collections.shuffle(bestCandidates, random);
            for (int j : bestCandidates) {
                addEdge(bestTeam, bestPot, j);
                if (solve()) {
                    return true;
                }
                removeEdge(bestTeam, bestPot, j);
            }
            return false;
        }

        private List<Integer> candidatesFor(int i, int p) {
            List<Integer> result = new ArrayList<>();
            int pi = potOf[i];
            Set<Integer> opp = opponents.get(i);
            Map<String, Integer> ccI = countryCount.get(i);
            for (int j = 0; j < n; j++) {
                if (j == i) continue;
                if (potOf[j] != p) continue;          // opponent must be from target pot
                if (demand[j][pi] <= 0) continue;      // opponent still needs a team from i's pot
                if (opp.contains(j)) continue;         // not already playing
                if (countryOf[i].equals(countryOf[j])) continue; // never same country
                if (ccI.getOrDefault(countryOf[j], 0) >= MAX_PER_COUNTRY) continue;
                if (countryCount.get(j).getOrDefault(countryOf[i], 0) >= MAX_PER_COUNTRY) continue;
                result.add(j);
            }
            return result;
        }

        private void addEdge(int i, int p, int j) {
            int pi = potOf[i];
            demand[i][p]--;
            demand[j][pi]--;
            opponents.get(i).add(j);
            opponents.get(j).add(i);
            bump(countryCount.get(i), countryOf[j], 1);
            bump(countryCount.get(j), countryOf[i], 1);
            edges.add(key(i, j));
        }

        private void removeEdge(int i, int p, int j) {
            int pi = potOf[i];
            demand[i][p]++;
            demand[j][pi]++;
            opponents.get(i).remove(j);
            opponents.get(j).remove(i);
            bump(countryCount.get(i), countryOf[j], -1);
            bump(countryCount.get(j), countryOf[i], -1);
            edges.remove(key(i, j));
        }

        private void bump(Map<String, Integer> map, String c, int delta) {
            int v = map.getOrDefault(c, 0) + delta;
            if (v <= 0) map.remove(c);
            else map.put(c, v);
        }

        private long key(int a, int b) {
            int lo = Math.min(a, b), hi = Math.max(a, b);
            return (long) lo * n + hi;
        }
    }

    // ============================================================
    //  Home/away orientation via directed cycles per pot-pair
    // ============================================================
    private List<int[]> orient(Set<Long> edges, int n, int[] potOf, Random random) {
        // Group edges by unordered pot pair so each group is 2-regular.
        Map<Integer, List<int[]>> groups = new HashMap<>();
        for (long e : edges) {
            int a = (int) (e / n);
            int b = (int) (e % n);
            int pa = potOf[a], pb = potOf[b];
            int gkey = Math.min(pa, pb) * POTS + Math.max(pa, pb);
            groups.computeIfAbsent(gkey, k -> new ArrayList<>()).add(new int[]{a, b});
        }

        List<int[]> directed = new ArrayList<>();
        for (List<int[]> group : groups.values()) {
            directed.addAll(orientGroup(group, random));
        }
        return directed;
    }

    /** Orient a 2-regular graph as directed cycles -> every vertex gets out-deg 1 and in-deg 1. */
    private List<int[]> orientGroup(List<int[]> groupEdges, Random random) {
        int m = groupEdges.size();
        boolean[] used = new boolean[m];
        // adjacency: vertex -> list of {neighbor, edgeIndex}
        Map<Integer, List<int[]>> adj = new HashMap<>();
        for (int idx = 0; idx < m; idx++) {
            int a = groupEdges.get(idx)[0];
            int b = groupEdges.get(idx)[1];
            adj.computeIfAbsent(a, k -> new ArrayList<>()).add(new int[]{b, idx});
            adj.computeIfAbsent(b, k -> new ArrayList<>()).add(new int[]{a, idx});
        }

        List<int[]> directed = new ArrayList<>(m);
        for (int startIdx = 0; startIdx < m; startIdx++) {
            if (used[startIdx]) continue;
            int current = groupEdges.get(startIdx)[0];
            // Walk the cycle, directing each edge current -> next (current is HOME).
            while (true) {
                int chosenEdge = -1, next = -1;
                for (int[] na : adj.get(current)) {
                    if (!used[na[1]]) {
                        next = na[0];
                        chosenEdge = na[1];
                        break;
                    }
                }
                if (chosenEdge == -1) break; // cycle complete
                used[chosenEdge] = true;
                directed.add(new int[]{current, next, 0}); // {home, away, matchday}
                current = next;
            }
        }
        return directed;
    }

    // ============================================================
    //  Matchday scheduling
    // ============================================================
    /**
     * Split the schedule into exactly {@code rounds} balanced matchdays.
     *
     * <p>The match graph is {@code rounds}-regular (every team plays exactly
     * {@code rounds} games), so a proper edge-colouring with {@code rounds} colours is a
     * 1-factorisation: each colour class is a perfect matching, i.e. a matchday in which
     * every team plays exactly once. For 36 teams that yields the real Champions-League
     * shape — 8 matchdays of 18 matches each, every matchday complete.
     *
     * <p>We try a randomised backtracking colouring a number of times. Almost every
     * regular graph of this degree is colourable with {@code rounds} colours; on the rare
     * graph that is not, we fall back to greedy first-fit so a usable (if less tidy)
     * schedule is still produced rather than failing the whole draw.
     */
    private int assignMatchdays(List<int[]> directed, int n, int rounds, Random random) {
        for (int attempt = 0; attempt < 400; attempt++) {
            int[] colouring = tryColourExact(directed, n, rounds, random);
            if (colouring != null) {
                for (int i = 0; i < directed.size(); i++) {
                    directed.get(i)[2] = colouring[i] + 1; // matchday is 1-based
                }
                return rounds;
            }
        }
        return assignMatchdaysGreedy(directed, n, random);
    }

    /**
     * Proper edge-colouring with exactly {@code colours} colours via randomised
     * backtracking. Returns colour[i] in {@code [0, colours)} for each match, or
     * {@code null} if no such colouring was found within the step budget.
     */
    private int[] tryColourExact(List<int[]> directed, int n, int colours, Random random) {
        int m = directed.size();
        int[] order = new int[m];
        for (int i = 0; i < m; i++) order[i] = i;
        // Shuffle the edge order so repeated attempts explore different search trees.
        for (int i = m - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = order[i]; order[i] = order[j]; order[j] = tmp;
        }
        int[] colour = new int[m];
        Arrays.fill(colour, -1);
        int[] usedMask = new int[n];        // bitmask of colours already used by each team
        int[] budget = {100 * m};
        if (colourEdges(0, order, directed, colour, usedMask, colours, budget, random)) {
            return colour;
        }
        return null;
    }

    private boolean colourEdges(int pos, int[] order, List<int[]> directed, int[] colour,
                                int[] usedMask, int colours, int[] budget, Random random) {
        if (pos == order.length) return true;
        if (--budget[0] < 0) return false;

        int e = order[pos];
        int u = directed.get(e)[0], v = directed.get(e)[1];
        int forbidden = usedMask[u] | usedMask[v];

        List<Integer> avail = new ArrayList<>();
        for (int c = 0; c < colours; c++) {
            if ((forbidden & (1 << c)) == 0) avail.add(c);
        }
        Collections.shuffle(avail, random);

        for (int c : avail) {
            int bit = 1 << c;
            colour[e] = c;
            usedMask[u] |= bit;
            usedMask[v] |= bit;
            if (colourEdges(pos + 1, order, directed, colour, usedMask, colours, budget, random)) {
                return true;
            }
            colour[e] = -1;
            usedMask[u] &= ~bit;
            usedMask[v] &= ~bit;
        }
        return false;
    }

    /** Greedy first-fit fallback (a team plays at most once per matchday; may use extra rounds). */
    private int assignMatchdaysGreedy(List<int[]> directed, int n, Random random) {
        Collections.shuffle(directed, random);
        List<boolean[]> busy = new ArrayList<>(); // busy[matchday] -> teams already playing that day
        int maxMatchday = 0;

        for (int[] match : directed) {
            int home = match[0], away = match[1];
            int md = 0;
            while (true) {
                if (md == busy.size()) {
                    busy.add(new boolean[n]);
                }
                boolean[] day = busy.get(md);
                if (!day[home] && !day[away]) {
                    day[home] = true;
                    day[away] = true;
                    break;
                }
                md++;
            }
            match[2] = md + 1; // store 1-based matchday in slot 2
            maxMatchday = Math.max(maxMatchday, md + 1);
        }
        return maxMatchday;
    }
}

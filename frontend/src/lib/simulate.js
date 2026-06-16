// Pure, client-side "what-if" simulation helpers.
//
// These never touch the backend — regular users can't persist results (writes
// are admin-only by design), so predictions live only in the browser. The logic
// here intentionally mirrors the server so projections match reality:
//   • standings tiebreak: points → goal difference → goals for → name
//     (see StandingsService.getStandings)
//   • knockout advancement: winner of slot s feeds slot s/2 of the next round,
//     as home when s is even (see KnockoutService.advanceWinner)

/** Is a prediction fully entered (both sides numeric)? */
export function hasPrediction(pred) {
  return (
    pred &&
    pred.home !== '' && pred.home != null &&
    pred.away !== '' && pred.away != null &&
    !Number.isNaN(Number(pred.home)) &&
    !Number.isNaN(Number(pred.away))
  );
}

/**
 * Effective score for a fixture: the user's prediction wins if entered,
 * otherwise the real result if the match has been played, else null (ignored).
 */
function effectiveScore(fixture, predictions) {
  const pred = predictions[fixture.id];
  if (hasPrediction(pred)) {
    return { home: Number(pred.home), away: Number(pred.away), source: 'predicted' };
  }
  if (fixture.played && fixture.homeScore != null && fixture.awayScore != null) {
    return { home: fixture.homeScore, away: fixture.awayScore, source: 'real' };
  }
  return null;
}

/**
 * Project the league table from real results + the user's predictions.
 * @returns array of rows ranked exactly like the server.
 */
export function projectStandings(fixtures, predictions) {
  const table = new Map(); // teamId -> accumulator

  const ensure = (id, name, country, pot) => {
    if (!table.has(id)) {
      table.set(id, {
        teamId: id, team: name, country, pot,
        played: 0, won: 0, drawn: 0, lost: 0,
        goalsFor: 0, goalsAgainst: 0, points: 0,
      });
    }
    return table.get(id);
  };

  for (const f of fixtures) {
    ensure(f.homeTeamId, f.homeTeam, f.homeCountry, f.homePot);
    ensure(f.awayTeamId, f.awayTeam, f.awayCountry, f.awayPot);

    const score = effectiveScore(f, predictions);
    if (!score) continue;

    const home = table.get(f.homeTeamId);
    const away = table.get(f.awayTeamId);
    const { home: hs, away: as } = score;

    home.played++; away.played++;
    home.goalsFor += hs; home.goalsAgainst += as;
    away.goalsFor += as; away.goalsAgainst += hs;

    if (hs > as) { home.won++; home.points += 3; away.lost++; }
    else if (hs < as) { away.won++; away.points += 3; home.lost++; }
    else { home.drawn++; away.drawn++; home.points++; away.points++; }
  }

  const rows = [...table.values()].map((a) => ({
    ...a,
    goalDifference: a.goalsFor - a.goalsAgainst,
  }));

  rows.sort((a, b) =>
    b.points - a.points ||
    b.goalDifference - a.goalDifference ||
    b.goalsFor - a.goalsFor ||
    a.team.localeCompare(b.team)
  );

  return rows.map((r, i) => ({ ...r, position: i + 1 }));
}

/** Random plausible score 0–4, weighted toward lower scores. */
function randomScore() {
  const r = Math.random();
  if (r < 0.30) return 0;
  if (r < 0.62) return 1;
  if (r < 0.84) return 2;
  if (r < 0.95) return 3;
  return 4;
}

/** Fill predictions for every not-yet-predicted, unplayed fixture. */
export function autoSimulateLeague(fixtures, predictions) {
  const next = { ...predictions };
  for (const f of fixtures) {
    if (hasPrediction(next[f.id]) || f.played) continue;
    next[f.id] = { home: randomScore(), away: randomScore() };
  }
  return next;
}

// ============================================================
//  Knockout
// ============================================================

const ROUND_LABELS = { 1: 'Final', 2: 'Semi-finals', 4: 'Quarter-finals', 8: 'Round of 16', 16: 'Round of 32' };
const labelFor = (matchCount) => ROUND_LABELS[matchCount] || `Round of ${matchCount * 2}`;

const key = (roundIndex, slot) => `${roundIndex}:${slot}`;
const teamSide = (match, side) => match[side];

/** The team object (home/away) that a match's pick selected, or null. */
function winnerOf(match) {
  if (match.winnerId == null) return null;
  if (match.home && match.home.id === match.winnerId) return match.home;
  if (match.away && match.away.id === match.winnerId) return match.away;
  return null;
}

/**
 * Build the full predicted bracket from a first-round seed and the user's picks.
 * @param seedMatches [{ slot, home:{id,name,seed}|null, away:{...}|null }] in slot order
 * @param picks       { "roundIndex:slot": winnerTeamId }
 * @returns { rounds:[{ index, label, matches:[{slot,home,away,winnerId}] }], champion }
 */
export function buildKnockout(seedMatches, picks) {
  const rounds = [];
  let current = seedMatches
    .slice()
    .sort((a, b) => a.slot - b.slot)
    .map((m) => ({ slot: m.slot, home: m.home, away: m.away }));
  let roundIndex = 0;

  while (true) {
    const withWinner = current.map((mt) => {
      const pick = picks[key(roundIndex, mt.slot)];
      const valid = pick != null && (mt.home?.id === pick || mt.away?.id === pick);
      return { ...mt, winnerId: valid ? pick : null };
    });
    rounds.push({ index: roundIndex, label: labelFor(withWinner.length), matches: withWinner });

    if (withWinner.length <= 1) break;

    const next = [];
    for (let s = 0; s < withWinner.length / 2; s++) {
      next.push({
        slot: s,
        home: winnerOf(withWinner[2 * s]),       // even slot → home
        away: winnerOf(withWinner[2 * s + 1]),   // odd slot  → away
      });
    }
    current = next;
    roundIndex++;
  }

  const finalMatch = rounds[rounds.length - 1].matches[0];
  return { rounds, champion: finalMatch ? winnerOf(finalMatch) : null };
}

/** Pick a random winner for every fully-decided match, round by round. */
export function randomKnockoutPicks(seedMatches) {
  const picks = {};
  let current = seedMatches
    .slice()
    .sort((a, b) => a.slot - b.slot)
    .map((m) => ({ slot: m.slot, home: m.home, away: m.away }));
  let roundIndex = 0;

  while (current.length >= 1) {
    const decided = current.map((mt) => {
      let winner = null;
      if (mt.home && mt.away) winner = Math.random() < 0.5 ? mt.home : mt.away;
      else winner = mt.home || mt.away || null;
      if (winner) picks[key(roundIndex, mt.slot)] = winner.id;
      return { ...mt, winnerId: winner?.id ?? null };
    });
    if (decided.length <= 1) break;
    const next = [];
    for (let s = 0; s < decided.length / 2; s++) {
      next.push({ slot: s, home: winnerOf(decided[2 * s]), away: winnerOf(decided[2 * s + 1]) });
    }
    current = next;
    roundIndex++;
  }
  return picks;
}

/** Largest power of two ≤ n, capped at 16 (the max bracket the server supports). */
export function bracketSize(teamCount) {
  let size = 1;
  while (size * 2 <= Math.min(teamCount, 16)) size *= 2;
  return size >= 2 ? size : 0;
}

/**
 * Seed a first round from ranked standings rows: pair seed (k+1) v (N-k),
 * higher seed at home — identical to KnockoutService.generate.
 */
export function seedFromStandings(rankedRows) {
  const n = bracketSize(rankedRows.length);
  if (!n) return [];
  const seeds = rankedRows.slice(0, n);
  const matches = [];
  for (let slot = 0; slot < n / 2; slot++) {
    const home = seeds[slot];
    const away = seeds[n - 1 - slot];
    matches.push({
      slot,
      home: { id: home.teamId, name: home.team, seed: slot + 1 },
      away: { id: away.teamId, name: away.team, seed: n - slot },
    });
  }
  return matches;
}

/** Map a server bracket's first round into seed-match shape. */
export function seedFromBracket(bracketRound) {
  return bracketRound.matches.map((m) => ({
    slot: m.slot,
    home: m.homeTeamId ? { id: m.homeTeamId, name: m.homeTeam, seed: m.homeSeed } : null,
    away: m.awayTeamId ? { id: m.awayTeamId, name: m.awayTeam, seed: m.awaySeed } : null,
  }));
}

/** Pre-fill picks from already-played knockout matches across the server bracket. */
export function picksFromBracket(bracketRounds) {
  const picks = {};
  bracketRounds.forEach((round, roundIndex) => {
    round.matches.forEach((m) => {
      if (m.played && m.winnerTeamId != null) picks[key(roundIndex, m.slot)] = m.winnerTeamId;
    });
  });
  return picks;
}

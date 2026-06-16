import { useEffect, useMemo, useState } from 'react';
import api from '../api/client.js';

const POT_LABELS = { POT1: 'Pot 1', POT2: 'Pot 2', POT3: 'Pot 3', POT4: 'Pot 4' };

export default function TeamSearchPage() {
  const [teams, setTeams] = useState([]);
  const [standings, setStandings] = useState([]);
  const [fixtures, setFixtures] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [query, setQuery] = useState('');
  const [selectedId, setSelectedId] = useState(null);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    Promise.all([
      api.get('/teams').then((r) => r.data),
      api.get('/standings').then((r) => r.data).catch(() => []),
      api.get('/fixtures').then((r) => r.data).catch(() => []),
    ])
      .then(([t, s, f]) => { setTeams(t); setStandings(s); setFixtures(f); })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  const matches = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return [];
    return teams
      .filter((t) => t.name.toLowerCase().includes(q) || (t.country || '').toLowerCase().includes(q))
      .slice(0, 8);
  }, [query, teams]);

  const selected = useMemo(() => teams.find((t) => t.id === selectedId) || null, [teams, selectedId]);

  const pick = (team) => {
    setSelectedId(team.id);
    setQuery(team.name);
    setOpen(false);
  };

  const onSubmit = (e) => {
    e.preventDefault();
    if (matches.length) pick(matches[0]);
  };

  if (loading) return <div className="card">Loading teams…</div>;
  if (error) return <div className="alert alert-error">{error}</div>;

  return (
    <div>
      <h1 className="page-title">Find a Team</h1>
      <p className="muted ts-intro">
        Search any club to see its league standing, form, results and upcoming fixtures — all
        computed live from the latest results.
      </p>

      <form className="ts-search" onSubmit={onSubmit} autoComplete="off">
        <div className="ts-search-box">
          <svg className="ts-search-icon" width="18" height="18" viewBox="0 0 24 24" fill="none">
            <circle cx="11" cy="11" r="7" stroke="currentColor" strokeWidth="2" />
            <path d="M20 20l-3.5-3.5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
          </svg>
          <input
            value={query}
            placeholder="Search by club or country…"
            onChange={(e) => { setQuery(e.target.value); setOpen(true); setSelectedId(null); }}
            onFocus={() => setOpen(true)}
          />
          {query && (
            <button type="button" className="ts-clear" onClick={() => { setQuery(''); setSelectedId(null); }}>×</button>
          )}
        </div>
        {open && matches.length > 0 && (
          <ul className="ts-results">
            {matches.map((t) => (
              <li key={t.id}>
                <button type="button" onClick={() => pick(t)}>
                  <span className="team-name">{t.name}</span>
                  <span className="muted small">{t.country} · {POT_LABELS[t.pot] || t.pot}</span>
                </button>
              </li>
            ))}
          </ul>
        )}
        {open && query.trim() && matches.length === 0 && (
          <ul className="ts-results"><li className="ts-empty muted">No teams match “{query}”.</li></ul>
        )}
      </form>

      {selected ? (
        <TeamDetail team={selected} standings={standings} fixtures={fixtures} />
      ) : (
        <div className="card empty ts-placeholder">
          <h3>Search for a club</h3>
          <p className="muted">Start typing above to look up a team’s full profile.</p>
        </div>
      )}
    </div>
  );
}

function TeamDetail({ team, standings, fixtures }) {
  const row = standings.find((s) => s.teamId === team.id) || null;

  // All fixtures involving this team, from its own perspective, in chronological order.
  const teamFixtures = useMemo(() => {
    return fixtures
      .filter((f) => f.homeTeamId === team.id || f.awayTeamId === team.id)
      .sort((a, b) => a.matchday - b.matchday || a.id - b.id)
      .map((f) => {
        const isHome = f.homeTeamId === team.id;
        const opponent = isHome ? f.awayTeam : f.homeTeam;
        const teamScore = isHome ? f.homeScore : f.awayScore;
        const oppScore = isHome ? f.awayScore : f.homeScore;
        let outcome = null;
        if (f.played && teamScore != null && oppScore != null) {
          outcome = teamScore > oppScore ? 'W' : teamScore < oppScore ? 'L' : 'D';
        }
        return { ...f, isHome, opponent, teamScore, oppScore, outcome };
      });
  }, [fixtures, team.id]);

  const results = teamFixtures.filter((f) => f.outcome);
  const upcoming = teamFixtures.filter((f) => !f.played);

  // Stats derived from the results entered so far.
  const stats = useMemo(() => {
    const s = { played: 0, won: 0, drawn: 0, lost: 0, gf: 0, ga: 0, cleanSheets: 0, biggestWin: null };
    for (const f of results) {
      s.played++;
      s.gf += f.teamScore; s.ga += f.oppScore;
      if (f.oppScore === 0) s.cleanSheets++;
      if (f.outcome === 'W') {
        s.won++;
        const margin = f.teamScore - f.oppScore;
        if (!s.biggestWin || margin > s.biggestWin.margin) {
          s.biggestWin = { margin, label: `${f.teamScore}–${f.oppScore} vs ${f.opponent}` };
        }
      } else if (f.outcome === 'D') s.drawn++;
      else s.lost++;
    }
    return s;
  }, [results]);

  const last5 = results.slice(-5).map((f) => f.outcome);
  const gd = stats.gf - stats.ga;

  return (
    <div className="team-detail">
      <div className="card td-header">
        <div className="td-crest">{team.name.slice(0, 2).toUpperCase()}</div>
        <div>
          <h2>{team.name}</h2>
          <span className="muted">{team.country} · {POT_LABELS[team.pot] || team.pot}</span>
        </div>
        {row && <div className="td-rank">#{row.position}<span className="muted small">in table</span></div>}
      </div>

      {/* Standings row */}
      <h3 className="td-section">League Standing</h3>
      {row ? (
        <div className="card table-wrap">
          <table className="table">
            <thead>
              <tr><th>Pos</th><th>P</th><th>W</th><th>D</th><th>L</th><th>GF</th><th>GA</th><th>GD</th><th>Pts</th></tr>
            </thead>
            <tbody>
              <tr>
                <td><strong>{row.position}</strong></td>
                <td>{row.played}</td><td>{row.won}</td><td>{row.drawn}</td><td>{row.lost}</td>
                <td>{row.goalsFor}</td><td>{row.goalsAgainst}</td>
                <td>{row.goalDifference > 0 ? `+${row.goalDifference}` : row.goalDifference}</td>
                <td><strong>{row.points}</strong></td>
              </tr>
            </tbody>
          </table>
        </div>
      ) : (
        <div className="card empty"><p className="muted">This team isn’t in the standings yet.</p></div>
      )}

      {/* Stats */}
      <h3 className="td-section">Stats <span className="muted small">(from results so far)</span></h3>
      <div className="stat-grid">
        <StatCard label="Played" value={stats.played} />
        <StatCard label="Won" value={stats.won} />
        <StatCard label="Drawn" value={stats.drawn} />
        <StatCard label="Lost" value={stats.lost} />
        <StatCard label="Goals for" value={stats.gf} />
        <StatCard label="Goals against" value={stats.ga} />
        <StatCard label="Goal diff" value={gd > 0 ? `+${gd}` : gd} />
        <StatCard label="Clean sheets" value={stats.cleanSheets} />
      </div>
      <div className="td-form-row card">
        <div>
          <span className="muted">Form (last 5)</span>
          {last5.length ? (
            <span className="form-badges">
              {last5.map((r, i) => <span key={i} className={`form-badge ${r.toLowerCase()}`}>{r}</span>)}
            </span>
          ) : <span className="muted small"> — no matches played</span>}
        </div>
        <div>
          <span className="muted">Biggest win</span>
          <strong className="td-biggest">{stats.biggestWin ? stats.biggestWin.label : '–'}</strong>
        </div>
      </div>

      <div className="two-col">
        {/* Previous results */}
        <div className="card">
          <h3>Previous Results</h3>
          {results.length === 0 ? (
            <p className="muted">No matches played yet.</p>
          ) : (
            <ul className="td-match-list">
              {results.slice().reverse().map((f) => (
                <li key={f.id}>
                  <span className={`form-badge ${f.outcome.toLowerCase()}`}>{f.outcome}</span>
                  <span className="td-md">MD{f.matchday}</span>
                  <span className="td-opp">{f.isHome ? 'vs' : '@'} {f.opponent}</span>
                  <span className="td-score">{f.teamScore}–{f.oppScore}</span>
                </li>
              ))}
            </ul>
          )}
        </div>

        {/* Upcoming fixtures */}
        <div className="card">
          <h3>Upcoming Fixtures</h3>
          {upcoming.length === 0 ? (
            <p className="muted">No upcoming fixtures.</p>
          ) : (
            <ul className="td-match-list">
              {upcoming.map((f) => (
                <li key={f.id}>
                  <span className="td-md">MD{f.matchday}</span>
                  <span className="td-opp">{f.isHome ? 'vs' : '@'} {f.opponent}</span>
                  <span className={`td-venue ${f.isHome ? 'home' : 'away'}`}>{f.isHome ? 'Home' : 'Away'}</span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}

function StatCard({ label, value }) {
  return (
    <div className="card stat-card">
      <div className="stat-value">{value}</div>
      <div className="stat-label">{label}</div>
    </div>
  );
}

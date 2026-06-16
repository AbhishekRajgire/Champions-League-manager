import { useEffect, useMemo, useState } from 'react';
import api from '../api/client.js';
import {
  projectStandings,
  autoSimulateLeague,
  buildKnockout,
  randomKnockoutPicks,
  seedFromStandings,
  seedFromBracket,
  picksFromBracket,
  hasPrediction,
} from '../lib/simulate.js';

const PRED_KEY = 'ucl_predictions';
const PICKS_KEY = 'ucl_ko_picks';

const loadJSON = (k, fallback) => {
  try { return JSON.parse(localStorage.getItem(k)) ?? fallback; } catch { return fallback; }
};

export default function SimulatorPage() {
  const [tab, setTab] = useState('league');

  const [fixtures, setFixtures] = useState([]);
  const [bracket, setBracket] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [predictions, setPredictions] = useState(() => loadJSON(PRED_KEY, {}));
  const [picks, setPicks] = useState(() => loadJSON(PICKS_KEY, {}));
  const [activeMd, setActiveMd] = useState(null);

  // Load fixtures + bracket once.
  useEffect(() => {
    Promise.all([
      api.get('/fixtures/by-matchday').then((r) => r.data),
      api.get('/knockout/bracket').then((r) => r.data).catch(() => null),
    ])
      .then(([byMd, br]) => {
        const flat = Object.values(byMd).flat();
        setFixtures(flat);
        setBracket(br);
        const days = Object.keys(byMd);
        if (days.length) setActiveMd(days[0]);
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  // Persist predictions / picks.
  useEffect(() => { localStorage.setItem(PRED_KEY, JSON.stringify(predictions)); }, [predictions]);
  useEffect(() => { localStorage.setItem(PICKS_KEY, JSON.stringify(picks)); }, [picks]);

  const projected = useMemo(() => projectStandings(fixtures, predictions), [fixtures, predictions]);

  // Knockout seed: prefer a real server bracket, else seed from the projected table.
  const { seedMatches, seededFrom } = useMemo(() => {
    if (bracket && bracket.rounds?.length) {
      return { seedMatches: seedFromBracket(bracket.rounds[0]), seededFrom: 'bracket' };
    }
    return { seedMatches: seedFromStandings(projected), seededFrom: 'standings' };
  }, [bracket, projected]);

  const ko = useMemo(() => buildKnockout(seedMatches, picks), [seedMatches, picks]);

  if (loading) return <div className="card">Loading simulator…</div>;
  if (error) return <div className="alert alert-error">{error}</div>;

  const predictedCount = fixtures.filter((f) => hasPrediction(predictions[f.id])).length;

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Match Simulator</h1>
        <div className="sim-toggle">
          <button className={`tab ${tab === 'league' ? 'active' : ''}`} onClick={() => setTab('league')}>
            League Predictor
          </button>
          <button className={`tab ${tab === 'knockout' ? 'active' : ''}`} onClick={() => setTab('knockout')}>
            Knockout Predictor
          </button>
        </div>
      </div>

      <p className="muted sim-intro">
        Predict scores and outcomes to see how the tournament could unfold. These are your private
        what-if predictions — they're saved in your browser and never change the official results.
      </p>

      {tab === 'league' ? (
        <LeagueTab
          fixtures={fixtures}
          predictions={predictions}
          setPredictions={setPredictions}
          projected={projected}
          activeMd={activeMd}
          setActiveMd={setActiveMd}
          predictedCount={predictedCount}
        />
      ) : (
        <KnockoutTab
          ko={ko}
          seedMatches={seedMatches}
          seededFrom={seededFrom}
          picks={picks}
          setPicks={setPicks}
        />
      )}
    </div>
  );
}

// ============================================================
//  League predictor
// ============================================================
function LeagueTab({ fixtures, predictions, setPredictions, projected, activeMd, setActiveMd, predictedCount }) {
  const byMd = useMemo(() => {
    const m = {};
    for (const f of fixtures) (m[f.matchday] ||= []).push(f);
    return m;
  }, [fixtures]);

  const matchdays = Object.keys(byMd).sort((a, b) => Number(a) - Number(b));

  if (fixtures.length === 0) {
    return (
      <div className="card empty">
        <h3>No fixtures to predict</h3>
        <p className="muted">An administrator needs to generate the league draw first.</p>
      </div>
    );
  }

  const setScore = (id, side, value) => {
    const v = value.replace(/\D/g, '').slice(0, 2);
    setPredictions((p) => ({ ...p, [id]: { ...p[id], [side]: v } }));
  };

  const autoSim = () => setPredictions((p) => autoSimulateLeague(fixtures, p));
  const clearAll = () => setPredictions({});

  const list = activeMd ? byMd[activeMd] || [] : [];

  return (
    <div className="sim-grid">
      <div>
        <div className="sim-actions">
          <button className="btn btn-primary" onClick={autoSim}>⚡ Auto-simulate remaining</button>
          <button className="btn btn-ghost" onClick={clearAll}>Clear predictions</button>
          <span className="muted small">{predictedCount} predicted</span>
        </div>

        <div className="tabs">
          {matchdays.map((md) => (
            <button key={md} className={`tab ${String(activeMd) === String(md) ? 'active' : ''}`} onClick={() => setActiveMd(md)}>
              MD {md}
            </button>
          ))}
        </div>

        <div className="fixture-list">
          {list.map((f) => {
            const pred = predictions[f.id] || {};
            return (
              <div key={f.id} className="fixture-row card">
                <div className="fx-team home"><span className="fx-name">{f.homeTeam}</span></div>
                <div className="fx-edit">
                  <input
                    className="score-input" inputMode="numeric"
                    placeholder={f.played ? String(f.homeScore) : '–'}
                    value={pred.home ?? ''}
                    onChange={(e) => setScore(f.id, 'home', e.target.value)}
                  />
                  <span>–</span>
                  <input
                    className="score-input" inputMode="numeric"
                    placeholder={f.played ? String(f.awayScore) : '–'}
                    value={pred.away ?? ''}
                    onChange={(e) => setScore(f.id, 'away', e.target.value)}
                  />
                </div>
                <div className="fx-team away"><span className="fx-name">{f.awayTeam}</span></div>
                <div className="fx-actions">
                  {hasPrediction(pred) ? <span className="played-tag predicted">✎</span>
                    : f.played ? <span className="played-tag">✓</span> : null}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      <div>
        <h3 className="sim-side-title">Projected Table</h3>
        <div className="card table-wrap">
          <table className="table compact">
            <thead>
              <tr><th>#</th><th className="left">Team</th><th>P</th><th>GD</th><th>Pts</th></tr>
            </thead>
            <tbody>
              {projected.map((r) => {
                const zone = r.position <= 8 ? 'zone-top' : r.position <= 24 ? 'zone-playoff' : 'zone-out';
                return (
                  <tr key={r.teamId} className={zone}>
                    <td>{r.position}</td>
                    <td className="left"><span className="team-name">{r.team}</span></td>
                    <td>{r.played}</td>
                    <td>{r.goalDifference > 0 ? `+${r.goalDifference}` : r.goalDifference}</td>
                    <td><strong>{r.points}</strong></td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
        <div className="legend">
          <span><i className="dot top" /> 1–8 R16</span>
          <span><i className="dot playoff" /> 9–24 Play-off</span>
          <span><i className="dot out" /> 25+ Out</span>
        </div>
      </div>
    </div>
  );
}

// ============================================================
//  Knockout predictor
// ============================================================
function KnockoutTab({ ko, seedMatches, seededFrom, picks, setPicks }) {
  if (!seedMatches.length) {
    return (
      <div className="card empty">
        <h3>Not enough teams for a knockout</h3>
        <p className="muted">Predict some league results first, or wait for an admin to seed the bracket.</p>
      </div>
    );
  }

  const pick = (roundIndex, slot, teamId) => {
    setPicks((p) => ({ ...p, [`${roundIndex}:${slot}`]: teamId }));
  };
  const randomSim = () => setPicks(randomKnockoutPicks(seedMatches));
  const clearAll = () => setPicks({});

  return (
    <div>
      <div className="sim-actions">
        <button className="btn btn-primary" onClick={randomSim}>🎲 Random simulate</button>
        <button className="btn btn-ghost" onClick={clearAll}>Clear picks</button>
        <span className="muted small">
          Seeded from {seededFrom === 'bracket' ? 'the official bracket' : 'your projected table'} · click a team to advance them
        </span>
      </div>

      {ko.champion && (
        <div className="champion-banner sim-champion">🏆 Predicted Champion: <strong>{ko.champion.name}</strong></div>
      )}

      <div className="bracket">
        {ko.rounds.map((round) => (
          <div key={round.index} className="bracket-round">
            <h3 className="bracket-round-title">{round.label}</h3>
            <div className="bracket-matches">
              {round.matches.map((m) => (
                <div key={m.slot} className="bracket-match card">
                  <KoSide team={m.home} winnerId={m.winnerId}
                    onPick={() => m.home && pick(round.index, m.slot, m.home.id)} />
                  <KoSide team={m.away} winnerId={m.winnerId}
                    onPick={() => m.away && pick(round.index, m.slot, m.away.id)} />
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function KoSide({ team, winnerId, onPick }) {
  const isWinner = team && winnerId === team.id;
  const isLoser = team && winnerId != null && winnerId !== team.id;
  return (
    <button
      type="button"
      className={`bracket-side ko-side ${team ? '' : 'tbd'} ${isWinner ? 'winner' : ''} ${isLoser ? 'loser' : ''}`}
      onClick={onPick}
      disabled={!team}
    >
      <span className="bracket-seed">{team?.seed ?? '–'}</span>
      <span className="bracket-team">{team?.name ?? 'TBD'}</span>
    </button>
  );
}

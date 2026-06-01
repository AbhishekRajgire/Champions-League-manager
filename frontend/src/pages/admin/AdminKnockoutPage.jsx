import { useEffect, useState } from 'react';
import api from '../../api/client.js';

const QUALIFIER_OPTIONS = [
  { value: 16, label: 'Round of 16 (top 16)' },
  { value: 8, label: 'Quarter-Finals (top 8)' },
  { value: 4, label: 'Semi-Finals (top 4)' },
  { value: 2, label: 'Final (top 2)' },
];

export default function AdminKnockoutPage() {
  const [bracket, setBracket] = useState(null);
  const [qualifiers, setQualifiers] = useState(16);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState(null);
  const [error, setError] = useState('');
  const [drafts, setDrafts] = useState({}); // matchId -> { home, away }

  const load = async () => {
    const { data } = await api.get('/knockout/bracket');
    setBracket(data);
  };

  useEffect(() => {
    load()
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  const notify = (msg) => {
    setMessage(msg);
    setError('');
    setTimeout(() => setMessage(null), 4000);
  };

  const generate = async () => {
    if (!confirm(`Seed the top ${qualifiers} teams into a fresh bracket? This replaces any existing knockout.`)) return;
    setBusy(true);
    setError('');
    try {
      await api.post(`/knockout/generate?qualifiers=${qualifiers}`);
      notify('Bracket generated from the current standings.');
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const reset = async () => {
    if (!confirm('Delete the entire knockout bracket?')) return;
    setBusy(true);
    try {
      await api.delete('/knockout');
      notify('Bracket cleared');
      setBracket({ champion: null, rounds: [] });
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const setDraft = (id, field, value) => {
    setDrafts((d) => ({ ...d, [id]: { ...d[id], [field]: value } }));
  };

  const saveResult = async (m) => {
    const draft = drafts[m.id] || {};
    const homeScore = draft.home ?? m.homeScore ?? 0;
    const awayScore = draft.away ?? m.awayScore ?? 0;
    try {
      await api.put(`/knockout/${m.id}/result`, {
        homeScore: Number(homeScore),
        awayScore: Number(awayScore),
      });
      notify(`Saved ${m.homeTeam} ${homeScore}-${awayScore} ${m.awayTeam}`);
      await load();
    } catch (err) {
      setError(err.message);
    }
  };

  if (loading) return <div className="card">Loading…</div>;

  const rounds = bracket?.rounds || [];

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Manage Knockout</h1>
        <div className="header-actions">
          <select
            value={qualifiers}
            onChange={(e) => setQualifiers(Number(e.target.value))}
            style={{ width: 'auto' }}
          >
            {QUALIFIER_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
          <button className="btn btn-primary" onClick={generate} disabled={busy}>
            {busy ? 'Working…' : 'Generate bracket'}
          </button>
          {rounds.length > 0 && (
            <button className="btn btn-danger" onClick={reset} disabled={busy}>
              Reset
            </button>
          )}
        </div>
      </div>

      {message && <div className="alert alert-success">{message}</div>}
      {error && <div className="alert alert-error">{error}</div>}

      {rounds.length === 0 ? (
        <div className="card empty">
          <h3>No bracket</h3>
          <p className="muted">
            Choose how many teams qualify and click <strong>Generate bracket</strong> to seed
            them from the standings (1 v N, 2 v N-1, …). Knockout matches cannot end level.
          </p>
        </div>
      ) : (
        rounds.map((round) => (
          <div key={round.round}>
            <h2 className="round-heading">{round.label}</h2>
            <div className="fixture-list">
              {round.matches.map((m) => {
                const ready = m.homeTeamId != null && m.awayTeamId != null;
                const draft = drafts[m.id] || {};
                return (
                  <div key={m.id} className="fixture-row admin card">
                    <div className="fx-team home">
                      <span className="fx-name">
                        {m.homeTeam ?? 'TBD'}
                        {m.homeSeed != null && <span className="badge">#{m.homeSeed}</span>}
                      </span>
                    </div>
                    <div className="fx-edit">
                      <input
                        type="number"
                        min="0"
                        className="score-input"
                        disabled={!ready}
                        value={draft.home ?? m.homeScore ?? ''}
                        onChange={(e) => setDraft(m.id, 'home', e.target.value)}
                      />
                      <span>–</span>
                      <input
                        type="number"
                        min="0"
                        className="score-input"
                        disabled={!ready}
                        value={draft.away ?? m.awayScore ?? ''}
                        onChange={(e) => setDraft(m.id, 'away', e.target.value)}
                      />
                    </div>
                    <div className="fx-team away">
                      <span className="fx-name">
                        {m.awaySeed != null && <span className="badge">#{m.awaySeed}</span>}
                        {m.awayTeam ?? 'TBD'}
                      </span>
                    </div>
                    <div className="fx-actions">
                      <button
                        className="btn btn-small btn-primary"
                        onClick={() => saveResult(m)}
                        disabled={!ready}
                      >
                        Save
                      </button>
                      {m.played && <span className="played-tag">✓</span>}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        ))
      )}
    </div>
  );
}

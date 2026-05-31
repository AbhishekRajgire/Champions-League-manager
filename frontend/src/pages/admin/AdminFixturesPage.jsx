import { useEffect, useState } from 'react';
import api from '../../api/client.js';

export default function AdminFixturesPage() {
  const [byMatchday, setByMatchday] = useState({});
  const [active, setActive] = useState(null);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [drafts, setDrafts] = useState({}); // fixtureId -> { home, away }

  const load = async () => {
    const { data } = await api.get('/fixtures/by-matchday');
    setByMatchday(data);
    const days = Object.keys(data);
    setActive((prev) => (prev && data[prev] ? prev : days[0] || null));
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
    if (!confirm('Generate a fresh draw? This replaces any existing fixtures and results.')) return;
    setBusy(true);
    setError('');
    try {
      const { data } = await api.post('/fixtures/generate');
      notify(`${data.message} ${data.fixtures} fixtures across ${data.matchdays} matchdays (solved in ${data.attempts} attempt(s)).`);
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const reset = async () => {
    if (!confirm('Delete ALL fixtures?')) return;
    setBusy(true);
    try {
      await api.delete('/fixtures');
      notify('Fixtures cleared');
      setByMatchday({});
      setActive(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const setDraft = (id, field, value) => {
    setDrafts((d) => ({ ...d, [id]: { ...d[id], [field]: value } }));
  };

  const saveResult = async (fixture) => {
    const draft = drafts[fixture.id] || {};
    const homeScore = draft.home ?? fixture.homeScore ?? 0;
    const awayScore = draft.away ?? fixture.awayScore ?? 0;
    try {
      await api.put(`/fixtures/${fixture.id}/result`, {
        homeScore: Number(homeScore),
        awayScore: Number(awayScore),
      });
      notify(`Saved ${fixture.homeTeam} ${homeScore}-${awayScore} ${fixture.awayTeam}`);
      await load();
    } catch (err) {
      setError(err.message);
    }
  };

  const clearResult = async (fixture) => {
    try {
      await api.patch(`/fixtures/${fixture.id}/clear`);
      notify('Result cleared');
      await load();
    } catch (err) {
      setError(err.message);
    }
  };

  if (loading) return <div className="card">Loading…</div>;

  const matchdays = Object.keys(byMatchday);
  const fixtures = active ? byMatchday[active] || [] : [];

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Manage Fixtures</h1>
        <div className="header-actions">
          <button className="btn btn-primary" onClick={generate} disabled={busy}>
            {busy ? 'Working…' : 'Generate draw'}
          </button>
          {matchdays.length > 0 && (
            <button className="btn btn-danger" onClick={reset} disabled={busy}>
              Reset
            </button>
          )}
        </div>
      </div>

      {message && <div className="alert alert-success">{message}</div>}
      {error && <div className="alert alert-error">{error}</div>}

      {matchdays.length === 0 ? (
        <div className="card empty">
          <h3>No fixtures</h3>
          <p className="muted">
            Make sure all four pots are filled with the same number of teams, then click
            <strong> Generate draw</strong>.
          </p>
        </div>
      ) : (
        <>
          <div className="tabs">
            {matchdays.map((md) => (
              <button
                key={md}
                className={`tab ${String(active) === String(md) ? 'active' : ''}`}
                onClick={() => setActive(md)}
              >
                MD {md}
              </button>
            ))}
          </div>

          <div className="fixture-list">
            {fixtures.map((f) => {
              const draft = drafts[f.id] || {};
              return (
                <div key={f.id} className="fixture-row admin card">
                  <div className="fx-team home">
                    <span className="fx-name">{f.homeTeam}</span>
                  </div>
                  <div className="fx-edit">
                    <input
                      type="number"
                      min="0"
                      className="score-input"
                      value={draft.home ?? f.homeScore ?? ''}
                      onChange={(e) => setDraft(f.id, 'home', e.target.value)}
                    />
                    <span>–</span>
                    <input
                      type="number"
                      min="0"
                      className="score-input"
                      value={draft.away ?? f.awayScore ?? ''}
                      onChange={(e) => setDraft(f.id, 'away', e.target.value)}
                    />
                  </div>
                  <div className="fx-team away">
                    <span className="fx-name">{f.awayTeam}</span>
                  </div>
                  <div className="fx-actions">
                    <button className="btn btn-small btn-primary" onClick={() => saveResult(f)}>
                      Save
                    </button>
                    {f.played && (
                      <button className="btn btn-small" onClick={() => clearResult(f)}>
                        Clear
                      </button>
                    )}
                    {f.played && <span className="played-tag">✓</span>}
                  </div>
                </div>
              );
            })}
          </div>
        </>
      )}
    </div>
  );
}

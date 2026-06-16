import { useEffect, useState } from 'react';
import api from '../api/client.js';
import { useAuth } from '../auth/AuthContext.jsx';

export default function ModeratorPage() {
  const { auth } = useAuth();
  const [tab, setTab] = useState('league');

  const [byMatchday, setByMatchday] = useState({});
  const [active, setActive] = useState(null);
  const [bracket, setBracket] = useState(null);
  const [drafts, setDrafts] = useState({});      // fixtureId -> { home, away }
  const [koDrafts, setKoDrafts] = useState({});   // knockoutId -> { home, away }
  const [myAudit, setMyAudit] = useState([]);

  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState(null);
  const [error, setError] = useState('');

  const notify = (m) => { setMessage(m); setError(''); setTimeout(() => setMessage(null), 4000); };

  const loadAll = async () => {
    const [md, br] = await Promise.all([
      api.get('/fixtures/by-matchday').then((r) => r.data),
      api.get('/knockout/bracket').then((r) => r.data).catch(() => null),
    ]);
    setByMatchday(md);
    setBracket(br);
    const days = Object.keys(md);
    setActive((prev) => (prev && md[prev] ? prev : days[0] || null));
    loadMyAudit();
  };

  const loadMyAudit = () => {
    api.get('/audit/mine').then(({ data }) => setMyAudit(data)).catch(() => {});
  };

  useEffect(() => {
    loadAll().catch((e) => setError(e.message)).finally(() => setLoading(false));
  }, []);

  const setDraft = (id, side, value) => {
    const v = value.replace(/\D/g, '').slice(0, 2);
    setDrafts((d) => ({ ...d, [id]: { ...d[id], [side]: v } }));
  };
  const setKoDraft = (id, side, value) => {
    const v = value.replace(/\D/g, '').slice(0, 2);
    setKoDrafts((d) => ({ ...d, [id]: { ...d[id], [side]: v } }));
  };

  const saveFixture = async (f) => {
    const d = drafts[f.id] || {};
    const home = d.home ?? f.homeScore;
    const away = d.away ?? f.awayScore;
    if (home === '' || home == null || away === '' || away == null) {
      setError(`Enter both scores for ${f.homeTeam} vs ${f.awayTeam}.`); return;
    }
    setBusy(true); setError('');
    try {
      await api.put(`/fixtures/${f.id}/result`, { homeScore: Number(home), awayScore: Number(away) });
      notify(`Saved ${f.homeTeam} ${home}-${away} ${f.awayTeam}`);
      await loadAll();
    } catch (err) { setError(err.message); } finally { setBusy(false); }
  };

  const clearFixture = async (f) => {
    setBusy(true); setError('');
    try {
      await api.patch(`/fixtures/${f.id}/clear`);
      setDrafts((d) => { const n = { ...d }; delete n[f.id]; return n; });
      notify('Result cleared'); await loadAll();
    } catch (err) { setError(err.message); } finally { setBusy(false); }
  };

  const saveMatchday = async () => {
    const list = active ? byMatchday[active] || [] : [];
    const toSave = list
      .map((f) => {
        const d = drafts[f.id] || {};
        const home = d.home ?? f.homeScore;
        const away = d.away ?? f.awayScore;
        return { f, home, away };
      })
      .filter(({ home, away }) => home !== '' && home != null && away !== '' && away != null);

    if (toSave.length === 0) { setError('No scores entered on this matchday.'); return; }
    setBusy(true); setError('');
    try {
      for (const { f, home, away } of toSave) {
        await api.put(`/fixtures/${f.id}/result`, { homeScore: Number(home), awayScore: Number(away) });
      }
      notify(`Saved ${toSave.length} result(s) on matchday ${active}.`);
      await loadAll();
    } catch (err) { setError(err.message); } finally { setBusy(false); }
  };

  const saveKnockout = async (m) => {
    const d = koDrafts[m.id] || {};
    const home = d.home ?? m.homeScore;
    const away = d.away ?? m.awayScore;
    if (home === '' || home == null || away === '' || away == null) {
      setError('Enter both scores.'); return;
    }
    if (Number(home) === Number(away)) { setError('Knockout matches need a winner — no draws.'); return; }
    setBusy(true); setError('');
    try {
      await api.put(`/knockout/${m.id}/result`, { homeScore: Number(home), awayScore: Number(away) });
      notify(`Saved ${m.homeTeam} ${home}-${away} ${m.awayTeam}`);
      await loadAll();
    } catch (err) { setError(err.message); } finally { setBusy(false); }
  };

  if (loading) return <div className="card">Loading results desk…</div>;

  const matchdays = Object.keys(byMatchday);
  const fixtures = active ? byMatchday[active] || [] : [];
  const koMatches = (bracket?.rounds || [])
    .flatMap((r) => r.matches.map((m) => ({ ...m, roundLabel: r.label })))
    .filter((m) => m.homeTeam && m.awayTeam);

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Results Desk</h1>
        <span className="user-chip">{auth.username} <em>{auth.role}</em></span>
      </div>
      <p className="muted sim-intro">
        Enter and clear match results. Team setup, draws and import/export are handled by admins.
      </p>

      {message && <div className="alert alert-success">{message}</div>}
      {error && <div className="alert alert-error">{error}</div>}

      <div className="sim-toggle">
        <button className={`tab ${tab === 'league' ? 'active' : ''}`} onClick={() => setTab('league')}>League Results</button>
        <button className={`tab ${tab === 'knockout' ? 'active' : ''}`} onClick={() => setTab('knockout')}>Knockout Results</button>
      </div>

      {tab === 'league' ? (
        matchdays.length === 0 ? (
          <div className="card empty"><h3>No fixtures yet</h3><p className="muted">An admin needs to generate the draw.</p></div>
        ) : (
          <>
            <div className="sim-actions">
              <button className="btn btn-primary" onClick={saveMatchday} disabled={busy}>
                {busy ? 'Saving…' : '💾 Save all on this matchday'}
              </button>
            </div>
            <div className="tabs">
              {matchdays.map((md) => (
                <button key={md} className={`tab ${String(active) === String(md) ? 'active' : ''}`} onClick={() => setActive(md)}>
                  MD {md}
                </button>
              ))}
            </div>
            <div className="fixture-list">
              {fixtures.map((f) => {
                const d = drafts[f.id] || {};
                return (
                  <div key={f.id} className="fixture-row admin card">
                    <div className="fx-team home"><span className="fx-name">{f.homeTeam}</span></div>
                    <div className="fx-edit">
                      <input className="score-input" inputMode="numeric"
                        value={d.home ?? f.homeScore ?? ''} onChange={(e) => setDraft(f.id, 'home', e.target.value)} />
                      <span>–</span>
                      <input className="score-input" inputMode="numeric"
                        value={d.away ?? f.awayScore ?? ''} onChange={(e) => setDraft(f.id, 'away', e.target.value)} />
                    </div>
                    <div className="fx-team away"><span className="fx-name">{f.awayTeam}</span></div>
                    <div className="fx-actions">
                      <button className="btn btn-small btn-primary" onClick={() => saveFixture(f)} disabled={busy}>Save</button>
                      {f.played && <button className="btn btn-small" onClick={() => clearFixture(f)} disabled={busy}>Clear</button>}
                      {f.played && <span className="played-tag">✓</span>}
                    </div>
                  </div>
                );
              })}
            </div>
          </>
        )
      ) : (
        koMatches.length === 0 ? (
          <div className="card empty"><h3>No playable knockout ties</h3><p className="muted">Ties appear here once both teams are decided. An admin seeds the bracket.</p></div>
        ) : (
          <div className="fixture-list">
            {koMatches.map((m) => {
              const d = koDrafts[m.id] || {};
              return (
                <div key={m.id} className="fixture-row admin card">
                  <div className="fx-team home"><span className="fx-name">{m.homeTeam}</span><span className="muted small">{m.roundLabel}</span></div>
                  <div className="fx-edit">
                    <input className="score-input" inputMode="numeric"
                      value={d.home ?? m.homeScore ?? ''} onChange={(e) => setKoDraft(m.id, 'home', e.target.value)} />
                    <span>–</span>
                    <input className="score-input" inputMode="numeric"
                      value={d.away ?? m.awayScore ?? ''} onChange={(e) => setKoDraft(m.id, 'away', e.target.value)} />
                  </div>
                  <div className="fx-team away"><span className="fx-name">{m.awayTeam}</span></div>
                  <div className="fx-actions">
                    <button className="btn btn-small btn-primary" onClick={() => saveKnockout(m)} disabled={busy}>Save</button>
                    {m.played && <span className="played-tag">✓ {m.winner} adv.</span>}
                  </div>
                </div>
              );
            })}
          </div>
        )
      )}

      {/* Own audit trail */}
      <div className="page-header td-section">
        <h3 style={{ margin: 0 }}>My recent changes</h3>
        <button className="btn btn-small" onClick={loadMyAudit}>Refresh</button>
      </div>
      <div className="card table-wrap">
        {myAudit.length === 0 ? (
          <p className="muted" style={{ padding: 8 }}>You haven’t made any changes yet.</p>
        ) : (
          <table className="table">
            <thead><tr><th className="left">When</th><th className="left">Action</th><th className="left">Target</th><th className="left">Change</th></tr></thead>
            <tbody>
              {myAudit.map((a) => (
                <tr key={a.id}>
                  <td className="left muted small">{formatTime(a.timestamp)}</td>
                  <td className="left"><span className="audit-action ok">{a.action}</span></td>
                  <td className="left">{a.target}</td>
                  <td className="left mono">{a.detail}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

function formatTime(iso) {
  try {
    return new Date(iso).toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
  } catch { return iso; }
}

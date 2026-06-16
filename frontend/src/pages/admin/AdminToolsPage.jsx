import { useEffect, useRef, useState } from 'react';
import api from '../../api/client.js';

export default function AdminToolsPage() {
  const [message, setMessage] = useState(null);
  const [error, setError] = useState('');

  const [teamsResult, setTeamsResult] = useState(null);
  const [resultsResult, setResultsResult] = useState(null);
  const [busy, setBusy] = useState('');

  const [audit, setAudit] = useState([]);
  const [auditLoading, setAuditLoading] = useState(true);

  const teamsInput = useRef(null);
  const resultsInput = useRef(null);

  const loadAudit = () => {
    setAuditLoading(true);
    api.get('/audit')
      .then(({ data }) => setAudit(data))
      .catch((e) => setError(e.message))
      .finally(() => setAuditLoading(false));
  };

  useEffect(loadAudit, []);

  const notify = (msg) => { setMessage(msg); setError(''); setTimeout(() => setMessage(null), 5000); };

  const importFile = async (path, inputRef, setResult, kind) => {
    const file = inputRef.current?.files?.[0];
    if (!file) { setError(`Choose a ${kind} CSV file first.`); return; }
    setBusy(kind); setError('');
    try {
      const fd = new FormData();
      fd.append('file', file);
      const { data } = await api.post(path, fd);
      setResult(data);
      notify(`${kind}: ${data.succeeded} applied, ${data.skipped} skipped (${data.rows} rows).`);
      loadAudit();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy('');
    }
  };

  const download = async (path, filename) => {
    setError('');
    try {
      const res = await api.get(path, { responseType: 'blob' });
      const url = URL.createObjectURL(res.data);
      const a = document.createElement('a');
      a.href = url;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div>
      <h1 className="page-title">Admin Tools</h1>
      {message && <div className="alert alert-success">{message}</div>}
      {error && <div className="alert alert-error">{error}</div>}

      {/* Import */}
      <h3 className="td-section">Import (CSV)</h3>
      <div className="two-col">
        <div className="card">
          <h3>Teams</h3>
          <p className="muted small">Columns: <code>name, country, pot, logoUrl</code> · pot = POT1–POT4 (or 1–4). Existing teams are skipped.</p>
          <div className="tool-file-row">
            <input type="file" accept=".csv,text/csv" ref={teamsInput} />
            <button className="btn btn-primary" disabled={busy === 'Teams'}
              onClick={() => importFile('/import/teams', teamsInput, setTeamsResult, 'Teams')}>
              {busy === 'Teams' ? 'Importing…' : 'Import teams'}
            </button>
          </div>
          <ImportSummary result={teamsResult} />
        </div>

        <div className="card">
          <h3>Results</h3>
          <p className="muted small">Columns: <code>matchday, homeTeam, awayTeam, homeScore, awayScore</code>. Matched to existing fixtures by name.</p>
          <div className="tool-file-row">
            <input type="file" accept=".csv,text/csv" ref={resultsInput} />
            <button className="btn btn-primary" disabled={busy === 'Results'}
              onClick={() => importFile('/import/results', resultsInput, setResultsResult, 'Results')}>
              {busy === 'Results' ? 'Importing…' : 'Import results'}
            </button>
          </div>
          <ImportSummary result={resultsResult} />
        </div>
      </div>

      {/* Export */}
      <h3 className="td-section">Export (CSV)</h3>
      <div className="card tool-export">
        <button className="btn btn-ghost" onClick={() => download('/export/standings', 'standings.csv')}>
          ⬇ Standings table
        </button>
        <button className="btn btn-ghost" onClick={() => download('/export/bracket', 'bracket.csv')}>
          ⬇ Knockout bracket
        </button>
      </div>

      {/* Audit log */}
      <div className="page-header td-section">
        <h3 style={{ margin: 0 }}>Audit Log</h3>
        <button className="btn btn-small" onClick={loadAudit} disabled={auditLoading}>
          {auditLoading ? 'Loading…' : 'Refresh'}
        </button>
      </div>
      <div className="card table-wrap">
        {audit.length === 0 ? (
          <p className="muted" style={{ padding: 8 }}>{auditLoading ? 'Loading…' : 'No changes recorded yet.'}</p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th className="left">When</th>
                <th className="left">User</th>
                <th className="left">Action</th>
                <th className="left">Target</th>
                <th className="left">Change</th>
              </tr>
            </thead>
            <tbody>
              {audit.map((a) => (
                <tr key={a.id}>
                  <td className="left muted small">{formatTime(a.timestamp)}</td>
                  <td className="left"><span className="user-chip">{a.username}</span></td>
                  <td className="left"><span className={`audit-action ${actionClass(a.action)}`}>{a.action}</span></td>
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

function ImportSummary({ result }) {
  if (!result) return null;
  return (
    <div className="tool-summary">
      <div className="tool-summary-head">
        <span className="ok">✓ {result.succeeded} applied</span>
        {result.skipped > 0 && <span className="skip">⚠ {result.skipped} skipped</span>}
        <span className="muted small">of {result.rows} rows</span>
      </div>
      {result.messages?.length > 0 && (
        <ul className="tool-messages">
          {result.messages.map((m, i) => <li key={i}>{m}</li>)}
        </ul>
      )}
    </div>
  );
}

function actionClass(action) {
  if (action?.includes('CLEARED')) return 'warn';
  if (action?.includes('IMPORT')) return 'info';
  return 'ok';
}

function formatTime(iso) {
  try {
    const d = new Date(iso);
    return d.toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
  } catch {
    return iso;
  }
}

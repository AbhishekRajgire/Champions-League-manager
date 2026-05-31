import { useEffect, useState } from 'react';
import api from '../../api/client.js';

const POTS = ['POT1', 'POT2', 'POT3', 'POT4'];
const POT_LABELS = { POT1: 'Pot 1', POT2: 'Pot 2', POT3: 'Pot 3', POT4: 'Pot 4' };
const emptyForm = { name: '', country: '', pot: 'POT1' };

export default function AdminTeamsPage() {
  const [teams, setTeams] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [message, setMessage] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const load = () => api.get('/teams').then(({ data }) => setTeams(data)).catch((e) => setError(e.message));

  useEffect(() => {
    load();
  }, []);

  const notify = (msg) => {
    setMessage(msg);
    setError('');
    setTimeout(() => setMessage(null), 3000);
  };

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      if (editingId) {
        await api.put(`/teams/${editingId}`, form);
        notify('Team updated');
      } else {
        await api.post('/teams', form);
        notify('Team registered');
      }
      setForm(emptyForm);
      setEditingId(null);
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const startEdit = (t) => {
    setEditingId(t.id);
    setForm({ name: t.name, country: t.country, pot: t.pot });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const cancelEdit = () => {
    setEditingId(null);
    setForm(emptyForm);
  };

  const remove = async (id) => {
    if (!confirm('Delete this team?')) return;
    try {
      await api.delete(`/teams/${id}`);
      notify('Team deleted');
      await load();
    } catch (err) {
      setError(err.message);
    }
  };

  const loadSample = async () => {
    if (!confirm('This replaces all teams and fixtures with the real 36-team UCL line-up. Continue?')) return;
    setBusy(true);
    try {
      await api.post('/teams/load-sample');
      notify('Loaded 36 sample teams');
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const counts = POTS.map((p) => teams.filter((t) => t.pot === p).length);

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Manage Teams</h1>
        <button className="btn btn-ghost" onClick={loadSample} disabled={busy}>
          Load sample (36 teams)
        </button>
      </div>

      <div className="pot-counts">
        {POTS.map((p, i) => (
          <span key={p} className="badge-count">
            {POT_LABELS[p]}: <strong>{counts[i]}</strong>
          </span>
        ))}
      </div>

      <div className="card form-card">
        <h3>{editingId ? 'Edit team' : 'Register a team'}</h3>
        <form onSubmit={submit} className="inline-form">
          <div>
            <label>Team name</label>
            <input
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              placeholder="e.g. Real Madrid"
            />
          </div>
          <div>
            <label>Country</label>
            <input
              value={form.country}
              onChange={(e) => setForm({ ...form, country: e.target.value })}
              placeholder="e.g. Spain"
            />
          </div>
          <div>
            <label>Pot</label>
            <select value={form.pot} onChange={(e) => setForm({ ...form, pot: e.target.value })}>
              {POTS.map((p) => (
                <option key={p} value={p}>
                  {POT_LABELS[p]}
                </option>
              ))}
            </select>
          </div>
          <div className="form-actions">
            <button className="btn btn-primary" disabled={busy}>
              {editingId ? 'Save' : 'Add team'}
            </button>
            {editingId && (
              <button type="button" className="btn btn-ghost" onClick={cancelEdit}>
                Cancel
              </button>
            )}
          </div>
        </form>
        {message && <div className="alert alert-success">{message}</div>}
        {error && <div className="alert alert-error">{error}</div>}
      </div>

      <div className="card table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th className="left">Team</th>
              <th className="left">Country</th>
              <th>Pot</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {teams.map((t) => (
              <tr key={t.id}>
                <td className="left">{t.name}</td>
                <td className="left">{t.country}</td>
                <td>{POT_LABELS[t.pot]}</td>
                <td>
                  <button className="btn btn-small" onClick={() => startEdit(t)}>
                    Edit
                  </button>
                  <button className="btn btn-small btn-danger" onClick={() => remove(t.id)}>
                    Delete
                  </button>
                </td>
              </tr>
            ))}
            {teams.length === 0 && (
              <tr>
                <td colSpan="4" className="muted">
                  No teams yet — register teams above or load the sample set.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

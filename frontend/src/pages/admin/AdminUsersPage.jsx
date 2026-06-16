import { useEffect, useState } from 'react';
import api from '../../api/client.js';
import { useAuth } from '../../auth/AuthContext.jsx';

const ROLES = ['ADMIN', 'MODERATOR', 'USER'];
const ROLE_BADGE = { ADMIN: 'warn', MODERATOR: 'info', USER: 'ok' };

export default function AdminUsersPage() {
  const { auth } = useAuth();
  const [users, setUsers] = useState([]);
  const [drafts, setDrafts] = useState({}); // id -> role
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState(null);
  const [message, setMessage] = useState(null);
  const [error, setError] = useState('');

  const notify = (m) => { setMessage(m); setError(''); setTimeout(() => setMessage(null), 4000); };

  const load = () => {
    setLoading(true);
    api.get('/users')
      .then(({ data }) => setUsers(data))
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  };
  useEffect(load, []);

  const saveRole = async (u) => {
    const role = drafts[u.id] ?? u.role;
    if (role === u.role) { setError('Pick a different role first.'); return; }
    setBusyId(u.id); setError('');
    try {
      await api.patch(`/users/${u.id}/role`, { role });
      notify(`${u.username} is now ${role}`);
      setDrafts((d) => { const n = { ...d }; delete n[u.id]; return n; });
      load();
    } catch (err) { setError(err.message); } finally { setBusyId(null); }
  };

  if (loading) return <div className="card">Loading users…</div>;

  return (
    <div>
      <h1 className="page-title">Manage Users</h1>
      <p className="muted sim-intro">
        Assign roles. <strong>Admins</strong> have full control, <strong>Moderators</strong> may enter results only,
        and <strong>Users</strong> get read-only access plus predictions.
      </p>
      {message && <div className="alert alert-success">{message}</div>}
      {error && <div className="alert alert-error">{error}</div>}

      <div className="card table-wrap">
        <table className="table">
          <thead>
            <tr><th className="left">Username</th><th className="left">Current role</th><th className="left">Change to</th><th></th></tr>
          </thead>
          <tbody>
            {users.map((u) => {
              const isSelf = u.username === auth.username;
              const selected = drafts[u.id] ?? u.role;
              return (
                <tr key={u.id}>
                  <td className="left team-name">
                    {u.username} {isSelf && <span className="muted small">(you)</span>}
                  </td>
                  <td className="left">
                    <span className={`audit-action ${ROLE_BADGE[u.role] || 'ok'}`}>{u.role}</span>
                  </td>
                  <td className="left">
                    <select
                      value={selected}
                      disabled={isSelf}
                      onChange={(e) => setDrafts((d) => ({ ...d, [u.id]: e.target.value }))}
                    >
                      {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
                    </select>
                  </td>
                  <td className="left">
                    {isSelf ? (
                      <span className="muted small">— can’t change own role</span>
                    ) : (
                      <button className="btn btn-small btn-primary"
                        disabled={busyId === u.id || selected === u.role}
                        onClick={() => saveRole(u)}>
                        {busyId === u.id ? 'Saving…' : 'Save'}
                      </button>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}

import { useEffect, useState } from 'react';
import api from '../api/client.js';

const POT_LABELS = { POT1: 'Pot 1', POT2: 'Pot 2', POT3: 'Pot 3', POT4: 'Pot 4' };

export default function TeamsPage() {
  const [teams, setTeams] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    api
      .get('/teams')
      .then(({ data }) => setTeams(data))
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="card">Loading teams…</div>;
  if (error) return <div className="alert alert-error">{error}</div>;

  const pots = ['POT1', 'POT2', 'POT3', 'POT4'];

  return (
    <div>
      <h1 className="page-title">Qualified Teams</h1>
      {teams.length === 0 ? (
        <div className="card empty">
          <h3>No teams registered</h3>
        </div>
      ) : (
        <div className="pots-grid">
          {pots.map((pot) => (
            <div key={pot} className="card pot-card">
              <h3>{POT_LABELS[pot]}</h3>
              <ul className="team-list">
                {teams
                  .filter((t) => t.pot === pot)
                  .map((t) => (
                    <li key={t.id}>
                      <span className="team-name">{t.name}</span>
                      <span className="muted small">{t.country}</span>
                    </li>
                  ))}
              </ul>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

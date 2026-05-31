import { useEffect, useState } from 'react';
import api from '../api/client.js';

export default function FixturesPage() {
  const [byMatchday, setByMatchday] = useState({});
  const [active, setActive] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    api
      .get('/fixtures/by-matchday')
      .then(({ data }) => {
        setByMatchday(data);
        const days = Object.keys(data);
        if (days.length) setActive(days[0]);
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="card">Loading fixtures…</div>;
  if (error) return <div className="alert alert-error">{error}</div>;

  const matchdays = Object.keys(byMatchday);
  if (matchdays.length === 0) {
    return (
      <div className="card empty">
        <h3>No fixtures yet</h3>
        <p className="muted">An administrator needs to register teams and generate the draw.</p>
      </div>
    );
  }

  const fixtures = byMatchday[active] || [];

  return (
    <div>
      <h1 className="page-title">Fixtures</h1>
      <div className="tabs">
        {matchdays.map((md) => (
          <button
            key={md}
            className={`tab ${String(active) === String(md) ? 'active' : ''}`}
            onClick={() => setActive(md)}
          >
            Matchday {md}
          </button>
        ))}
      </div>

      <div className="fixture-list">
        {fixtures.map((f) => (
          <div key={f.id} className="fixture-row card">
            <div className="fx-team home">
              <span className="fx-name">{f.homeTeam}</span>
              <span className="badge">{f.homePot.replace('POT', 'P')}</span>
            </div>
            <div className="fx-score">
              {f.played ? (
                <strong>
                  {f.homeScore} – {f.awayScore}
                </strong>
              ) : (
                <span className="vs">vs</span>
              )}
            </div>
            <div className="fx-team away">
              <span className="badge">{f.awayPot.replace('POT', 'P')}</span>
              <span className="fx-name">{f.awayTeam}</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

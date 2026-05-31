import { useEffect, useState } from 'react';
import api from '../api/client.js';

export default function StatsPage() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    api
      .get('/stats')
      .then(({ data }) => setStats(data))
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="card">Loading stats…</div>;
  if (error) return <div className="alert alert-error">{error}</div>;

  return (
    <div>
      <h1 className="page-title">Statistics</h1>
      <div className="stat-grid">
        <StatCard label="Teams" value={stats.totalTeams} />
        <StatCard label="Total fixtures" value={stats.totalFixtures} />
        <StatCard label="Played" value={stats.playedFixtures} />
        <StatCard label="Remaining" value={stats.remainingFixtures} />
        <StatCard label="Total goals" value={stats.totalGoals} />
        <StatCard label="Goals / match" value={stats.averageGoalsPerMatch} />
      </div>

      <div className="card big-stat">
        <span className="muted">Biggest win</span>
        <h3>{stats.biggestWinScoreline}</h3>
      </div>

      <div className="two-col">
        <div className="card">
          <h3>Top scoring teams</h3>
          <ol className="rank-list">
            {stats.topScoringTeams.map((t) => (
              <li key={t.team}>
                <span>{t.team}</span>
                <strong>{t.value}</strong>
              </li>
            ))}
            {stats.topScoringTeams.length === 0 && <p className="muted">No matches played yet.</p>}
          </ol>
        </div>
        <div className="card">
          <h3>Best defences (fewest conceded)</h3>
          <ol className="rank-list">
            {stats.bestDefences.map((t) => (
              <li key={t.team}>
                <span>{t.team}</span>
                <strong>{t.value}</strong>
              </li>
            ))}
            {stats.bestDefences.length === 0 && <p className="muted">No matches played yet.</p>}
          </ol>
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

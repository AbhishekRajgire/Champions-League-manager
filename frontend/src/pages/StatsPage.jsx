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

  const playedForm = stats.teamForms.filter((f) => f.last5.length > 0);

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

      <div className="big-stat-row">
        <BigStat label="Biggest win" value={stats.biggestWinScoreline} />
        <BigStat label="Biggest home win" value={stats.biggestHomeWinScoreline} />
        <BigStat label="Biggest away win" value={stats.biggestAwayWinScoreline} />
      </div>

      <div className="two-col">
        <BigStat label="Most wins" value={teamStat(stats.mostWins)} />
        <BigStat label="Most losses" value={teamStat(stats.mostLosses)} />
      </div>

      <div className="two-col">
        <RankList title="Top scoring teams" items={stats.topScoringTeams} />
        <RankList title="Best defences (fewest conceded)" items={stats.bestDefences} />
      </div>

      <div className="two-col">
        <RankList title="Most clean sheets" items={stats.mostCleanSheets} />
        <div className="card">
          <h3>Form guide (last 5)</h3>
          {playedForm.length === 0 ? (
            <p className="muted">No matches played yet.</p>
          ) : (
            <table className="table">
              <thead>
                <tr>
                  <th className="left">Team</th>
                  <th>Streak</th>
                  <th>Form</th>
                </tr>
              </thead>
              <tbody>
                {playedForm.map((f) => (
                  <tr key={f.team}>
                    <td className="left team-name">{f.team}</td>
                    <td>
                      {f.streakLength > 0 ? (
                        <span className="streak-tag">{f.streakType}{f.streakLength}</span>
                      ) : (
                        '–'
                      )}
                    </td>
                    <td>
                      <span className="form-badges">
                        {f.last5.map((r, i) => (
                          <span key={i} className={`form-badge ${r.toLowerCase()}`}>{r}</span>
                        ))}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}

function teamStat(stat) {
  return stat ? `${stat.team} (${stat.value})` : '–';
}

function StatCard({ label, value }) {
  return (
    <div className="card stat-card">
      <div className="stat-value">{value}</div>
      <div className="stat-label">{label}</div>
    </div>
  );
}

function BigStat({ label, value }) {
  return (
    <div className="card big-stat">
      <span className="muted">{label}</span>
      <h3>{value}</h3>
    </div>
  );
}

function RankList({ title, items }) {
  return (
    <div className="card">
      <h3>{title}</h3>
      <ol className="rank-list">
        {items.map((t) => (
          <li key={t.team}>
            <span>{t.team}</span>
            <strong>{t.value}</strong>
          </li>
        ))}
        {items.length === 0 && <p className="muted">No matches played yet.</p>}
      </ol>
    </div>
  );
}

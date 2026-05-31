import { useEffect, useState } from 'react';
import api from '../api/client.js';

export default function StandingsPage() {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    api
      .get('/standings')
      .then(({ data }) => setRows(data))
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="card">Loading table…</div>;
  if (error) return <div className="alert alert-error">{error}</div>;
  if (rows.length === 0)
    return (
      <div className="card empty">
        <h3>No standings yet</h3>
        <p className="muted">Register teams to populate the league table.</p>
      </div>
    );

  // Top 8 qualify directly; 9-24 play-off; 25+ eliminated (UCL format cue lines).
  return (
    <div>
      <h1 className="page-title">League Phase Table</h1>
      <div className="card table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>#</th>
              <th className="left">Team</th>
              <th>P</th>
              <th>W</th>
              <th>D</th>
              <th>L</th>
              <th>GF</th>
              <th>GA</th>
              <th>GD</th>
              <th>Pts</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => {
              let zone = '';
              if (r.position <= 8) zone = 'zone-top';
              else if (r.position <= 24) zone = 'zone-playoff';
              else zone = 'zone-out';
              return (
                <tr key={r.teamId} className={zone}>
                  <td>{r.position}</td>
                  <td className="left">
                    <span className="team-name">{r.team}</span>
                    <span className="muted small"> · {r.country}</span>
                  </td>
                  <td>{r.played}</td>
                  <td>{r.won}</td>
                  <td>{r.drawn}</td>
                  <td>{r.lost}</td>
                  <td>{r.goalsFor}</td>
                  <td>{r.goalsAgainst}</td>
                  <td>{r.goalDifference > 0 ? `+${r.goalDifference}` : r.goalDifference}</td>
                  <td>
                    <strong>{r.points}</strong>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      <div className="legend">
        <span><i className="dot top" /> 1–8 Round of 16</span>
        <span><i className="dot playoff" /> 9–24 Knockout play-off</span>
        <span><i className="dot out" /> 25+ Eliminated</span>
      </div>
    </div>
  );
}

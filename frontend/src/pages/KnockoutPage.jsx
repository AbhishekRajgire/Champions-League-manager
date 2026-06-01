import { useEffect, useState } from 'react';
import api from '../api/client.js';

export default function KnockoutPage() {
  const [bracket, setBracket] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    api
      .get('/knockout/bracket')
      .then(({ data }) => setBracket(data))
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="card">Loading bracket…</div>;
  if (error) return <div className="alert alert-error">{error}</div>;
  if (!bracket || bracket.rounds.length === 0)
    return (
      <div className="card empty">
        <h3>No knockout bracket yet</h3>
        <p className="muted">An admin can seed the bracket once the league phase is complete.</p>
      </div>
    );

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Knockout Phase</h1>
        {bracket.champion && (
          <div className="champion-banner">🏆 Champion: <strong>{bracket.champion}</strong></div>
        )}
      </div>

      <div className="bracket">
        {bracket.rounds.map((round) => (
          <div key={round.round} className="bracket-round">
            <h3 className="bracket-round-title">{round.label}</h3>
            <div className="bracket-matches">
              {round.matches.map((m) => (
                <BracketMatch key={m.id} match={m} />
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function BracketMatch({ match }) {
  return (
    <div className="bracket-match card">
      <BracketSide
        team={match.homeTeam}
        seed={match.homeSeed}
        score={match.homeScore}
        winner={match.played && match.winnerTeamId === match.homeTeamId}
      />
      <BracketSide
        team={match.awayTeam}
        seed={match.awaySeed}
        score={match.awayScore}
        winner={match.played && match.winnerTeamId === match.awayTeamId}
      />
    </div>
  );
}

function BracketSide({ team, seed, score, winner }) {
  return (
    <div className={`bracket-side ${winner ? 'winner' : ''} ${team ? '' : 'tbd'}`}>
      <span className="bracket-seed">{seed ?? '–'}</span>
      <span className="bracket-team">{team ?? 'TBD'}</span>
      <span className="bracket-score">{score ?? ''}</span>
    </div>
  );
}

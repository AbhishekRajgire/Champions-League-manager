import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import '../landing.css';

export default function LandingPage() {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const rootRef = useRef(null);

  // Gated navigation: send guests to login (remembering where they wanted to go).
  const goAuthed = (path) =>
    isAuthenticated ? navigate(path) : navigate('/login', { state: { from: path } });

  const [scoreA, setScoreA] = useState(2);
  const [scoreB, setScoreB] = useState(1);
  const [probA, setProbA] = useState(62);

  // Reveal-on-scroll
  useEffect(() => {
    const root = rootRef.current;
    if (!root) return;
    const io = new IntersectionObserver(
      (entries) => entries.forEach((e) => e.isIntersecting && e.target.classList.add('in')),
      { threshold: 0.12 }
    );
    root.querySelectorAll('.reveal').forEach((el) => io.observe(el));
    return () => io.disconnect();
  }, []);

  const simulate = () => {
    // Simulating a prediction requires an account.
    if (!isAuthenticated) {
      navigate('/login', { state: { from: '/' } });
      return;
    }
    setScoreA(Math.floor(Math.random() * 4));
    setScoreB(Math.floor(Math.random() * 4));
    setProbA(45 + Math.floor(Math.random() * 30));
  };

  return (
    <div className="landing" ref={rootRef}>
      {/* Ambient background */}
      <div className="bg-layer" aria-hidden="true">
        <div className="glow blue" />
        <div className="glow purple" />
        <div className="glow cyan" />
      </div>
      <div className="grid-overlay" aria-hidden="true" />
      <svg className="stadium" viewBox="0 0 1100 360" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
        <defs>
          <radialGradient id="pitch" cx="50%" cy="20%" r="80%">
            <stop offset="0%" stopColor="#1e3a8a" stopOpacity="0.5" />
            <stop offset="100%" stopColor="#0b1024" stopOpacity="0" />
          </radialGradient>
          <linearGradient id="line" x1="0" y1="0" x2="1" y2="0">
            <stop offset="0%" stopColor="#38bdf8" stopOpacity="0" />
            <stop offset="50%" stopColor="#8b5cf6" stopOpacity="0.55" />
            <stop offset="100%" stopColor="#38bdf8" stopOpacity="0" />
          </linearGradient>
        </defs>
        <ellipse cx="550" cy="40" rx="520" ry="150" fill="url(#pitch)" />
        <ellipse cx="550" cy="60" rx="430" ry="120" stroke="#3b4a7a" strokeOpacity="0.3" strokeWidth="1.5" />
        <ellipse cx="550" cy="60" rx="330" ry="92" stroke="#3b4a7a" strokeOpacity="0.35" strokeWidth="1.5" />
        <ellipse cx="550" cy="60" rx="230" ry="64" stroke="#38bdf8" strokeOpacity="0.3" strokeWidth="1.5" />
        <ellipse cx="550" cy="60" rx="120" ry="34" stroke="#8b5cf6" strokeOpacity="0.5" strokeWidth="1.5" />
        <line x1="430" y1="60" x2="670" y2="60" stroke="#8b5cf6" strokeOpacity="0.4" strokeWidth="1.5" />
        <path d="M40 230 H170 V180 H300" stroke="url(#line)" strokeWidth="1.5" fill="none" />
        <path d="M40 300 H170 V180" stroke="url(#line)" strokeWidth="1.5" fill="none" />
        <path d="M1060 230 H930 V180 H800" stroke="url(#line)" strokeWidth="1.5" fill="none" />
        <path d="M1060 300 H930 V180" stroke="url(#line)" strokeWidth="1.5" fill="none" />
      </svg>

      {/* Nav */}
      <nav>
        <div className="l-wrap nav-inner">
          <div className="brand">
            <span className="logo">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M12 2l2.4 4.9 5.4.8-3.9 3.8.9 5.4L12 15.4 7.2 17l.9-5.4L4.2 7.7l5.4-.8L12 2z" fill="#fff" /></svg>
            </span>
            UCL Manager
          </div>
          <div className="nav-links">
            <a onClick={() => document.getElementById('features')?.scrollIntoView({ behavior: 'smooth' })}>Features</a>
            <a onClick={() => document.getElementById('predict')?.scrollIntoView({ behavior: 'smooth' })}>Predict</a>
            <a onClick={() => document.getElementById('bracket')?.scrollIntoView({ behavior: 'smooth' })}>Bracket</a>
            <a onClick={() => document.getElementById('standings')?.scrollIntoView({ behavior: 'smooth' })}>Standings</a>
          </div>
          <button className="nav-cta" onClick={() => goAuthed('/fixtures')}>Launch App</button>
        </div>
      </nav>

      {/* Hero */}
      <header className="hero l-wrap">
        <span className="pill"><span className="dot" /> Live · Quarter Finals in progress</span>
        <h1>Champions League<br /><span className="accent">Manager</span></h1>
        <p className="sub">Simulate, predict, and control the tournament — from the group stage to the final whistle.</p>
        <div className="hero-btns">
          <button className="l-btn btn-primary" onClick={() => goAuthed('/simulator')}>
            <svg width="17" height="17" viewBox="0 0 24 24" fill="none"><path d="M5 3l14 9-14 9V3z" fill="#fff" /></svg>
            Start Simulation
          </button>
          <button className="l-btn btn-ghost" onClick={() => goAuthed('/standings')}>View Standings →</button>
        </div>
      </header>

      {/* Live stat cards */}
      <section className="l-wrap reveal">
        <div className="stats">
          <div className="glass stat">
            <div className="ico"><svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M12 2v20M2 12h20" stroke="currentColor" strokeWidth="2" /></svg></div>
            <div className="label">Current Stage</div>
            <div className="value">Quarter Finals</div>
            <span className="badge">8 teams left</span>
          </div>
          <div className="glass stat">
            <div className="ico"><svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M12 2l2.4 4.9 5.4.8-3.9 3.8.9 5.4L12 15.4 7.2 17l.9-5.4L4.2 7.7l5.4-.8L12 2z" fill="currentColor" /></svg></div>
            <div className="label">Top Team</div>
            <div className="value">Real Madrid</div>
            <span className="badge">18 pts · GD +14</span>
          </div>
          <div className="glass stat">
            <div className="ico"><svg width="18" height="18" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="8" r="4" stroke="currentColor" strokeWidth="2" /><path d="M4 21c0-4 4-6 8-6s8 2 8 6" stroke="currentColor" strokeWidth="2" /></svg></div>
            <div className="label">Top Scorer</div>
            <div className="value">Mbappé <small>· 9</small></div>
            <span className="badge">3 assists</span>
          </div>
          <div className="glass stat">
            <div className="ico"><svg width="18" height="18" viewBox="0 0 24 24" fill="none"><rect x="3" y="4" width="18" height="16" rx="2" stroke="currentColor" strokeWidth="2" /><path d="M3 9h18" stroke="currentColor" strokeWidth="2" /></svg></div>
            <div className="label">Matches Played</div>
            <div className="value">96 <small>/ 125</small></div>
            <span className="badge">77% complete</span>
          </div>
        </div>
      </section>

      {/* Features */}
      <section className="section l-wrap reveal" id="features">
        <div className="eyebrow">Everything you need</div>
        <h2>Run the tournament your way</h2>
        <p className="lead">A modern control room for the world's biggest club competition.</p>
        <div className="features">
          {[
            { t: 'Smart Simulation', d: 'Run thousands of match outcomes powered by team strength models.', i: <path d="M13 2L4 14h6l-1 8 9-12h-6l1-8z" fill="currentColor" /> },
            { t: 'Predictions', d: 'Probabilistic forecasts for every fixture, stage, and the final.', i: <><path d="M3 17l5-5 4 4 8-8" stroke="currentColor" strokeWidth="2" fill="none" /><path d="M16 8h5v5" stroke="currentColor" strokeWidth="2" fill="none" /></> },
            { t: 'Team Stats', d: 'Deep dive into form, goals, defence and head-to-head records.', i: <path d="M4 20V10M10 20V4M16 20v-7M22 20V8" stroke="currentColor" strokeWidth="2" /> },
            { t: 'What-if Scenarios', d: 'Tweak any result and instantly see the bracket reshape.', i: <><circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="2" /><path d="M12 7v5l3 2" stroke="currentColor" strokeWidth="2" /></> },
          ].map((f) => (
            <div className="glass feature" key={f.t}>
              <div className="fi"><svg width="22" height="22" viewBox="0 0 24 24" fill="none">{f.i}</svg></div>
              <h3>{f.t}</h3>
              <p>{f.d}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Interactive: prediction + bracket */}
      <section className="section l-wrap reveal" id="predict">
        <div className="eyebrow">Interactive</div>
        <h2>Predict the next clash</h2>
        <div className="interactive">
          {/* Prediction card */}
          <div className="glass panel">
            <div className="panel-title">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M3 17l5-5 4 4 8-8" stroke="#38bdf8" strokeWidth="2" fill="none" /></svg>
              Match Prediction
              <span className="tag">Quarter Final · 1st leg</span>
            </div>
            <div className="match">
              <div className="team-side">
                <div className="team-logo logo-a">M</div>
                <div className="name">Man City</div>
                <div className="seed">Seed #1</div>
              </div>
              <div className="vs">VS</div>
              <div className="team-side">
                <div className="team-logo logo-b">B</div>
                <div className="name">Bayern</div>
                <div className="seed">Seed #4</div>
              </div>
            </div>
            <div className="score-row">
              <input className="score-input" value={scoreA} inputMode="numeric"
                onChange={(e) => setScoreA(e.target.value.replace(/\D/g, '').slice(0, 2))} />
              <span className="vs">—</span>
              <input className="score-input" value={scoreB} inputMode="numeric"
                onChange={(e) => setScoreB(e.target.value.replace(/\D/g, '').slice(0, 2))} />
            </div>
            <button className="l-btn btn-primary btn-sim" onClick={simulate}>
              {isAuthenticated ? (
                <>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none"><path d="M5 3l14 9-14 9V3z" fill="#fff" /></svg>
                  Simulate Match
                </>
              ) : (
                <>
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none"><rect x="5" y="11" width="14" height="9" rx="2" stroke="#fff" strokeWidth="2" /><path d="M8 11V8a4 4 0 0 1 8 0v3" stroke="#fff" strokeWidth="2" /></svg>
                  Log in to Simulate
                </>
              )}
            </button>
            <div className="prob-bar"><div className="prob-fill" style={{ width: `${probA}%` }} /></div>
            <div className="prob-legend"><span>Man City {probA}%</span><span>Bayern {100 - probA}%</span></div>
          </div>

          {/* Bracket */}
          <div className="glass panel" id="bracket">
            <div className="panel-title">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M4 6h6v12H4M14 9h6v6h-6M10 12h4" stroke="#8b5cf6" strokeWidth="2" fill="none" /></svg>
              Knockout Bracket
              <span className="tag">Live preview</span>
            </div>
            <div className="bracket">
              <div className="bcol">
                <div className="bmatch"><div className="bteam win">Man City <span className="s">2</span></div><div className="bteam">Bayern <span>1</span></div></div>
                <div className="bmatch"><div className="bteam win">Real Madrid <span className="s">3</span></div><div className="bteam">Arsenal <span>1</span></div></div>
                <div className="bmatch"><div className="bteam">PSG <span>0</span></div><div className="bteam win">Inter <span className="s">2</span></div></div>
                <div className="bmatch"><div className="bteam win">Liverpool <span className="s">1</span></div><div className="bteam">Barça <span>0</span></div></div>
              </div>
              <div className="bcol">
                <div className="bmatch"><div className="bteam win">Man City <span className="s">2</span></div><div className="bteam">Real <span>2</span></div></div>
                <div className="bmatch"><div className="bteam">Inter <span>1</span></div><div className="bteam win">Liverpool <span className="s">2</span></div></div>
              </div>
              <div className="bcol">
                <div className="bmatch final"><div className="bteam win">Man City <span className="s">?</span></div><div className="bteam">Liverpool <span>?</span></div></div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* League table */}
      <section className="section l-wrap reveal" id="standings">
        <div className="eyebrow">Standings</div>
        <h2>League phase table</h2>
        <div className="glass panel locked-wrap" style={{ marginTop: 40, maxWidth: 760, marginLeft: 'auto', marginRight: 'auto' }}>
          {!isAuthenticated && (
            <div className="lock-overlay">
              <div className="lock-icon">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none"><rect x="5" y="11" width="14" height="9" rx="2" stroke="currentColor" strokeWidth="2" /><path d="M8 11V8a4 4 0 0 1 8 0v3" stroke="currentColor" strokeWidth="2" /></svg>
              </div>
              <div className="lock-title">Log in to unlock</div>
              <p className="lock-sub">See the complete standings table and full tournament stats.</p>
              <button className="l-btn btn-primary" onClick={() => navigate('/login', { state: { from: '/standings' } })}>Log in to view full table</button>
            </div>
          )}
          <table className={`l-table${!isAuthenticated ? ' blurred' : ''}`}>
            <thead><tr><th>#</th><th>Club</th><th>P</th><th>W</th><th>D</th><th>L</th><th>GD</th><th style={{ textAlign: 'right' }}>Pts</th></tr></thead>
            <tbody>
              {[
                { r: 1, n: 'Real Madrid', a: 'RM', c: '#1e3a8a', p: 8, w: 6, d: 0, l: 2, gd: '+14', pts: 18, top: true },
                { r: 2, n: 'Man City', a: 'MC', c: '#0c4a6e', p: 8, w: 5, d: 2, l: 1, gd: '+11', pts: 17, top: true },
                { r: 3, n: 'Liverpool', a: 'LV', c: '#7f1d1d', p: 8, w: 5, d: 1, l: 2, gd: '+9', pts: 16 },
                { r: 4, n: 'Inter', a: 'IN', c: '#3730a3', p: 8, w: 4, d: 2, l: 2, gd: '+6', pts: 14 },
                { r: 5, n: 'Bayern', a: 'BY', c: '#831843', p: 8, w: 4, d: 1, l: 3, gd: '+5', pts: 13 },
              ].map((t) => (
                <tr key={t.r} className={t.top ? 'top' : ''}>
                  <td>{t.r}</td>
                  <td><div className="tname"><span className="chip" style={{ background: t.c }}>{t.a}</span>{t.n}</div></td>
                  <td>{t.p}</td><td>{t.w}</td><td>{t.d}</td><td>{t.l}</td><td>{t.gd}</td>
                  <td className="pts">{t.pts}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {isAuthenticated && (
          <div style={{ textAlign: 'center', marginTop: 22 }}>
            <button className="l-btn btn-ghost" onClick={() => navigate('/standings')}>View full table &amp; stats →</button>
          </div>
        )}
      </section>

      {/* Predicted champion */}
      <section className="l-wrap reveal">
        <div className="glass champion">
          <div className="crown">★ Predicted Champion ★</div>
          <div className="champ-logo">MC</div>
          <h3>Manchester City</h3>
          <div className="winprob">Win probability · 28.4%</div>
        </div>
      </section>

      {/* Footer */}
      <footer>
        <div className="l-wrap foot-inner">
          <div className="brand">
            <span className="logo"><svg width="16" height="16" viewBox="0 0 24 24" fill="none"><path d="M12 2l2.4 4.9 5.4.8-3.9 3.8.9 5.4L12 15.4 7.2 17l.9-5.4L4.2 7.7l5.4-.8L12 2z" fill="#fff" /></svg></span>
            UCL Manager
          </div>
          <span>© 2026 Champions League Manager — a simulation showcase.</span>
        </div>
      </footer>
    </div>
  );
}

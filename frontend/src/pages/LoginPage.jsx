import { useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = location.state?.from;
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const data = await login(username, password);
      navigate(from || (data.role === 'ADMIN' ? '/admin/fixtures' : '/fixtures'));
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-card card">
      <h2>Sign in</h2>
      <p className="muted">Champions League Manager</p>
      <form onSubmit={submit}>
        <label>Username</label>
        <input value={username} onChange={(e) => setUsername(e.target.value)} autoFocus />
        <label>Password</label>
        <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
        {error && <div className="alert alert-error">{error}</div>}
        <button className="btn btn-primary full" disabled={loading}>
          {loading ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
      <p className="muted small">
        No account? <Link to="/register" state={from ? { from } : undefined}>Register as a fan</Link>
      </p>
      <div className="hint">
        <strong>Demo accounts</strong>
        <div>admin / admin123 &nbsp;·&nbsp; user / user123</div>
      </div>
    </div>
  );
}

import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';

export default function Navbar() {
  const { auth, isAuthenticated, isAdmin, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="navbar">
      <div className="navbar-brand">
        <span className="brand-star">★</span> UCL Manager
      </div>
      <div className="navbar-links">
        <NavLink to="/fixtures">Fixtures</NavLink>
        <NavLink to="/standings">Table</NavLink>
        <NavLink to="/knockout">Knockout</NavLink>
        <NavLink to="/stats">Stats</NavLink>
        <NavLink to="/teams">Teams</NavLink>
        {isAdmin && <NavLink to="/admin/teams">Manage Teams</NavLink>}
        {isAdmin && <NavLink to="/admin/fixtures">Manage Fixtures</NavLink>}
        {isAdmin && <NavLink to="/admin/knockout">Manage Knockout</NavLink>}
      </div>
      <div className="navbar-user">
        {isAuthenticated ? (
          <>
            <span className="user-chip">
              {auth.username} <em>{auth.role}</em>
            </span>
            <button className="btn btn-ghost" onClick={handleLogout}>
              Logout
            </button>
          </>
        ) : (
          <NavLink to="/login" className="btn btn-primary">
            Login
          </NavLink>
        )}
      </div>
    </nav>
  );
}

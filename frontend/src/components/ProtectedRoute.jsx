import { Navigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';

/**
 * Gate a route by authentication and (optionally) role.
 * - adminOnly: restrict to ADMIN.
 * - roles: restrict to any of the given role names (e.g. ['ADMIN','MODERATOR']).
 */
export default function ProtectedRoute({ children, adminOnly = false, roles }) {
  const { isAuthenticated, isAdmin, auth } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  if (adminOnly && !isAdmin) {
    return <Navigate to="/fixtures" replace />;
  }
  if (roles && !roles.includes(auth.role)) {
    return <Navigate to="/fixtures" replace />;
  }
  return children;
}

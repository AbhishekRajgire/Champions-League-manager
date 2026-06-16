import { Routes, Route, Navigate, useLocation } from 'react-router-dom';
import Navbar from './components/Navbar.jsx';
import ProtectedRoute from './components/ProtectedRoute.jsx';
import LandingPage from './pages/LandingPage.jsx';
import SimulatorPage from './pages/SimulatorPage.jsx';
import TeamSearchPage from './pages/TeamSearchPage.jsx';
import ModeratorPage from './pages/ModeratorPage.jsx';
import LoginPage from './pages/LoginPage.jsx';
import RegisterPage from './pages/RegisterPage.jsx';
import FixturesPage from './pages/FixturesPage.jsx';
import StandingsPage from './pages/StandingsPage.jsx';
import StatsPage from './pages/StatsPage.jsx';
import TeamsPage from './pages/TeamsPage.jsx';
import KnockoutPage from './pages/KnockoutPage.jsx';
import AdminTeamsPage from './pages/admin/AdminTeamsPage.jsx';
import AdminFixturesPage from './pages/admin/AdminFixturesPage.jsx';
import AdminKnockoutPage from './pages/admin/AdminKnockoutPage.jsx';
import AdminToolsPage from './pages/admin/AdminToolsPage.jsx';
import AdminUsersPage from './pages/admin/AdminUsersPage.jsx';

export default function App() {
  const location = useLocation();
  const isLanding = location.pathname === '/';

  if (isLanding) {
    return (
      <Routes>
        <Route path="/" element={<LandingPage />} />
      </Routes>
    );
  }

  return (
    <>
      <Navbar />
      <main className="container">
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          {/* Public read-only views */}
          <Route path="/fixtures" element={<FixturesPage />} />
          <Route path="/standings" element={<StandingsPage />} />
          <Route path="/stats" element={<StatsPage />} />
          <Route path="/teams" element={<TeamsPage />} />
          <Route path="/knockout" element={<KnockoutPage />} />

          {/* Requires login (any role) */}
          <Route
            path="/simulator"
            element={
              <ProtectedRoute>
                <SimulatorPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/team-search"
            element={
              <ProtectedRoute>
                <TeamSearchPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/moderator"
            element={
              <ProtectedRoute roles={['ADMIN', 'MODERATOR']}>
                <ModeratorPage />
              </ProtectedRoute>
            }
          />

          {/* Admin only */}
          <Route
            path="/admin/teams"
            element={
              <ProtectedRoute adminOnly>
                <AdminTeamsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/fixtures"
            element={
              <ProtectedRoute adminOnly>
                <AdminFixturesPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/knockout"
            element={
              <ProtectedRoute adminOnly>
                <AdminKnockoutPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/tools"
            element={
              <ProtectedRoute adminOnly>
                <AdminToolsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/users"
            element={
              <ProtectedRoute adminOnly>
                <AdminUsersPage />
              </ProtectedRoute>
            }
          />

          <Route path="*" element={<Navigate to="/fixtures" replace />} />
        </Routes>
      </main>
    </>
  );
}

import { Routes, Route, Navigate } from 'react-router-dom';
import Navbar from './components/Navbar.jsx';
import ProtectedRoute from './components/ProtectedRoute.jsx';
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

export default function App() {
  return (
    <>
      <Navbar />
      <main className="container">
        <Routes>
          <Route path="/" element={<Navigate to="/fixtures" replace />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          {/* Public read-only views */}
          <Route path="/fixtures" element={<FixturesPage />} />
          <Route path="/standings" element={<StandingsPage />} />
          <Route path="/stats" element={<StatsPage />} />
          <Route path="/teams" element={<TeamsPage />} />
          <Route path="/knockout" element={<KnockoutPage />} />

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

          <Route path="*" element={<Navigate to="/fixtures" replace />} />
        </Routes>
      </main>
    </>
  );
}

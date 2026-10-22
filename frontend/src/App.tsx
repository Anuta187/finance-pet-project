import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth';
import { Layout } from './components/Layout';
import { RequireAuth, RequireRole } from './components/Guards';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { DashboardPage } from './pages/DashboardPage';
import { TransfersPage } from './pages/TransfersPage';
import { PaymentsPage } from './pages/PaymentsPage';
import { CardsPage } from './pages/CardsPage';
import { AutopaymentsPage } from './pages/AutopaymentsPage';
import { HistoryPage } from './pages/HistoryPage';
import { AdminPage } from './pages/AdminPage';
import { NotFoundPage } from './pages/NotFoundPage';

function Home() {
  const { user } = useAuth();
  return user?.role === 'ADMIN' ? <Navigate to="/admin" replace /> : <DashboardPage />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route
        element={
          <RequireAuth>
            <Layout />
          </RequireAuth>
        }
      >
        <Route index element={<Home />} />
        <Route path="transfers" element={<TransfersPage />} />
        <Route path="payments" element={<PaymentsPage />} />
        <Route path="cards" element={<CardsPage />} />
        <Route path="autopayments" element={<AutopaymentsPage />} />
        <Route path="history" element={<HistoryPage />} />
        <Route
          path="admin"
          element={
            <RequireRole role="ADMIN">
              <AdminPage />
            </RequireRole>
          }
        />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}

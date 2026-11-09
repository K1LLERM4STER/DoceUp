import { useEffect } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { SessionProvider, useSession } from './session';
import { ErrorState, LoadingState } from '../components/ui';
import AppRoutes from './routes';

export function RequireSession() {
  const { user, loading, error, reload, goToLogin } = useSession();
  const { pathname } = useLocation();
  useEffect(() => {
    if (!loading && !error && !user) goToLogin();
  }, [loading, error, user, pathname]);
  if (loading) return <main className="auth-state"><LoadingState>Conferindo sua sessão…</LoadingState></main>;
  if (error) return <main className="auth-state"><ErrorState error={error} retry={reload} /></main>;
  return user ? <Outlet /> : null;
}

function NavigationEffects() {
  const { pathname } = useLocation();
  useEffect(() => {
    window.scrollTo({ top: 0 });
    document.getElementById('conteudo')?.focus({ preventScroll: true });
  }, [pathname]);
  return null;
}

export default function App() {
  return <SessionProvider>
    <NavigationEffects />
    <AppRoutes />
  </SessionProvider>;
}

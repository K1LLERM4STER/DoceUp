import { createContext, useContext, useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { clearCsrf, logout, onUnauthorized } from '../api/client';
import { me } from '../api/auth';
import { useLoad } from './hooks';

const Session = createContext(null);
const intentionKey = 'doceup.return-path';

export function readIntention() {
  try {
    const path = sessionStorage.getItem(intentionKey);
    sessionStorage.removeItem(intentionKey);
    if (path && /^\/(?![\/\\])/.test(path) && !/[\\\u0000-\u001f]/.test(path) && !path.startsWith('/login')) return path;
  } catch {
    // The login still works with browser storage disabled.
  }
  return '/';
}

export function SessionProvider({ children }) {
  const navigate = useNavigate();
  const location = useLocation();
  const [expired, setExpired] = useState(false);
  const session = useLoad(async (signal) => {
    try {
      return await me(signal);
    } catch (error) {
      if (error.status === 401) return null;
      throw error;
    }
  }, []);
  function goToLogin(remember = true) {
    if (remember && location.pathname !== '/login') {
      try {
        sessionStorage.setItem(intentionKey, location.pathname + location.search);
      } catch {
        // Browser storage is optional for the return path.
      }
    }
    navigate('/login', { replace: true });
  }
  useEffect(() => {
    onUnauthorized(() => {
      setExpired(true);
      goToLogin();
    });
    return () => onUnauthorized(undefined);
  }, [location.pathname, location.search, navigate]);
  async function signOut() {
    await logout();
    clearCsrf();
    setExpired(true);
    goToLogin(false);
  }
  return <Session.Provider value={{ ...session, user: expired ? null : session.data, goToLogin, signOut }}>
    {children}
  </Session.Provider>;
}

export const useSession = () => useContext(Session);

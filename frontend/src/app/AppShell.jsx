import { useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { Button, ErrorState, Icon } from '../components/ui';
import { useSession } from './session';

const menu = [
  { to: '/', label: 'Início', icon: 'inicio' },
  { to: '/produtos', label: 'Produtos', icon: 'produtos' },
  { to: '/ingredientes', label: 'Ingredientes', icon: 'ingredientes' },
  { to: '/pedidos', label: 'Pedidos', icon: 'pedidos' },
  { to: '/vendas', label: 'Vendas', icon: 'vendas' },
];

function Navigation({ className }) {
  const { pathname } = useLocation();
  return <nav className={className} aria-label="Navegação principal">
    {menu.map(({ to, label, icon }) => {
      const production = to === '/produtos' && pathname.startsWith('/producao/');
      return <NavLink key={to} to={to} end={to === '/'}
        aria-current={production ? 'page' : undefined}
        className={({ isActive }) => isActive || production ? 'nav-link nav-active' : 'nav-link'}>
        <Icon name={icon} /><span>{label}</span>
      </NavLink>;
    })}
  </nav>;
}

export function BottomNav() { return <Navigation className="bottom-nav" />; }
export function Sidebar() { return <aside className="sidebar"><Navigation className="side-nav" /></aside>; }

export default function AppShell() {
  const { signOut } = useSession();
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  async function exit() {
    setBusy(true);
    setError(null);
    try {
      await signOut();
    } catch (failure) {
      setError(failure);
      setBusy(false);
    }
  }
  return <>
    <a className="skip-link" href="#conteudo">Ir para o conteúdo</a>
    <header className="app-header">
      <NavLink to="/" className="brand"><span className="brand-icon"><Icon name="marca" /></span>DoceUp</NavLink>
      <Button variant="ghost" onClick={exit} disabled={busy}><Icon name="sair" size={20} />Sair</Button>
    </header>
    <Sidebar />
    <main id="conteudo" className="app-main" tabIndex={-1}>
      <ErrorState error={error} /><Outlet />
    </main>
    <BottomNav />
  </>;
}

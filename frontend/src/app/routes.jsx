import { Route, Routes } from 'react-router-dom';
import { RequireSession } from './App';
import AppShell from './AppShell';
import Login from '../features/entrada/Login';
import Inicio from '../features/inicio/Inicio';
import { Produtos, ProdutoForm } from '../features/produtos/Produtos';
import Ingredientes from '../features/ingredientes/Ingredientes';
import Producao from '../features/producao/Producao';
import { NovaEncomenda, Pedidos } from '../features/pedidos/Pedidos';
import { Vendas, NovaVenda, VendaDetalhe } from '../features/vendas/Vendas';
import { Button, EmptyState, PageHeader } from '../components/ui';

function NotFound() {
  return <>
    <PageHeader title="Página não encontrada" />
    <EmptyState title="Não encontramos esta página" action={<Button to="/">Voltar ao início</Button>} />
  </>;
}

export default function AppRoutes() {
  return <Routes>
    <Route path="/login" element={<Login />} />
    <Route element={<RequireSession />}>
      <Route element={<AppShell />}>
        <Route index element={<Inicio />} />
        <Route path="produtos" element={<Produtos />} />
        <Route path="produtos/novo" element={<ProdutoForm />} />
        <Route path="produtos/:id/editar" element={<ProdutoForm />} />
        <Route path="ingredientes" element={<Ingredientes />} />
        <Route path="producao/:produtoId" element={<Producao />} />
        <Route path="pedidos" element={<Pedidos />} />
        <Route path="pedidos/novo" element={<NovaEncomenda />} />
        <Route path="vendas" element={<Vendas />} />
        <Route path="vendas/nova" element={<NovaVenda />} />
        <Route path="vendas/:id" element={<VendaDetalhe />} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Route>
  </Routes>;
}

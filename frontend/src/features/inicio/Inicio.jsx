import { useState } from 'react';
import { Link } from 'react-router-dom';
import { painel } from '../../api/painel';
import { pedidos } from '../../api/pedidos';
import { formatDay, formatMonth, shiftMonth, today } from '../../app/format';
import { useLoad } from '../../app/hooks';
import { Button, Card, ErrorState, Icon, LoadingState, PageHeader, StatCard, TextField } from '../../components/ui';

export default function Inicio() {
  const currentDay = today();
  const currentMonth = currentDay.slice(0, 7);
  const [mes, setMes] = useState(currentMonth);
  const [dia, setDia] = useState('');
  const result = useLoad((signal) => painel(dia ? { dia } : { mes }, signal), [mes, dia]);
  const data = result.data;
  const entregas = useLoad((signal) => pedidos(0, 'ABERTA', currentDay, signal), [currentDay]);
  const totalEntregas = entregas.data?.totalItens;
  const period = dia ? formatDay(dia) : formatMonth(mes);
  return <>
    <PageHeader title={dia ? 'Seu dia' : 'Seu mês'} subtitle="Acompanhe o que entrou e o que saiu." />
    <section className="dashboard" aria-label="Resumo financeiro">
      <div className="period-controls">
        <div className="period-picker" aria-label="Mês selecionado">
          <Button variant="ghost" aria-label="Mês anterior" onClick={() => { setMes(shiftMonth(mes, -1)); setDia(''); }}>
            <Icon name="esquerda" size={18} />
          </Button>
          <span><Icon name="pedidos" size={19} />{period}</span>
          <Button variant="ghost" aria-label="Próximo mês" disabled={mes >= currentMonth}
            onClick={() => { setMes(shiftMonth(mes, 1)); setDia(''); }}>
            <Icon name="direita" size={18} />
          </Button>
        </div>
        <TextField type="date" label="Consultar um dia" value={dia} max={currentDay} onChange={(value) => {
          setDia(value);
          if (value) setMes(value.slice(0, 7));
        }} />
        {(dia || mes !== currentMonth) && <Button variant="secondary" onClick={() => { setDia(''); setMes(currentMonth); }}>
          Mês atual
        </Button>}
      </div>
      {result.loading && <LoadingState />}
      <ErrorState error={result.error} retry={result.reload} />
      {data && <div className="stats-grid">
        <StatCard label={dia ? 'Saldo do dia' : 'Saldo do mês'} value={data.saldo} featured explanation="Recebimentos menos gastos" />
        <StatCard label="Recebimentos" value={data.recebimentos} />
        <StatCard label="Gastos" value={data.gastos} />
      </div>}
    </section>
    <section className="dashboard-actions" aria-label="Atalhos">
      <Link to="/vendas/nova"><Icon name="vendas" size={25} /><span>Registrar venda</span></Link>
      <Link to="/ingredientes?acao=compra"><Icon name="ingredientes" size={25} /><span>Adicionar compra</span></Link>
      <Link to="/produtos"><Icon name="producao" size={25} /><span>Produzir</span></Link>
    </section>
    <section className="dashboard-follow">
      <h2>Pra acompanhar</h2>
      <Card>
        <Link to="/vendas" className="follow-link"><Icon name="carteira" />
          <span>{data?.pagamentosPendentes === undefined ? 'Ver pagamentos pendentes' :
            data.pagamentosPendentes === 1 ? '1 pagamento pendente' : data.pagamentosPendentes + ' pagamentos pendentes'}</span>
          <Icon name="direita" size={17} />
        </Link>
      </Card>
      <Card>
        <Link to={'/pedidos?dataEntrega=' + currentDay} className="follow-link"><Icon name="pedidos" />
          <span>{totalEntregas === undefined ? 'Ver encomendas de hoje' :
            totalEntregas === 1 ? '1 encomenda para hoje' : totalEntregas + ' encomendas para hoje'}</span>
          <Icon name="direita" size={17} />
        </Link>
      </Card>
    </section>
  </>;
}

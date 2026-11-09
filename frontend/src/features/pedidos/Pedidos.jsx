import { useState } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { normalizePage } from '../../api/client';
import { cancelarPedido, concluirPedido, criarPedido, pedidos } from '../../api/pedidos';
import { todosProdutos } from '../../api/produtos';
import { brl, cents, formatDay, today } from '../../app/format';
import { useAction, useLoad } from '../../app/hooks';
import { Alert, Button, Card, ConfirmDialog, EmptyState, ErrorState, Icon, LoadingState,
  PageHeader, Pagination, QuantityInput, SelectField, StatusBadge, TextField } from '../../components/ui';

const estados = [
  { value: 'ABERTA', label: 'Em aberto' },
  { value: 'CONCLUIDA', label: 'Concluídas' },
];

function entregaValida(valor) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(valor ?? '')) return false;
  const data = new Date(valor + 'T12:00:00Z');
  return !Number.isNaN(data.getTime()) && data.toISOString().slice(0, 10) === valor;
}

export function Pedidos() {
  const [params, setParams] = useSearchParams();
  const { state } = useLocation();
  const status = params.get('status') === 'CONCLUIDA' ? 'CONCLUIDA' : 'ABERTA';
  const filter = params.get('dataEntrega');
  const dataEntrega = status === 'ABERTA' && entregaValida(filter) ? filter : undefined;
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState(null);
  const [notice, setNotice] = useState(state?.notice);
  const result = useLoad((signal) => pedidos(page, status, dataEntrega, signal).then(normalizePage),
    [page, status, dataEntrega]);

  function mostrar(next) {
    setPage(0);
    setParams(next === 'CONCLUIDA' ? { status: next } : {});
  }

  function afterUpdate(concluir) {
    setSelected(null);
    setNotice(concluir ? 'Encomenda concluída. O valor entrou nos recebimentos.' :
      'Encomenda cancelada. Os doces voltaram para venda.');
    setPage(0);
    result.reload();
  }

  return <>
    <PageHeader title="Encomendas" subtitle="Separe os doces para cada entrega."
      action={<Button to="/pedidos/novo"><Icon name="mais" size={19} /> Nova encomenda</Button>} />
    <div className="order-tabs" role="group" aria-label="Filtrar encomendas">
      {estados.map((tab) => <Button key={tab.value} variant={status === tab.value ? 'primary' : 'secondary'}
        aria-pressed={status === tab.value} onClick={() => mostrar(tab.value)}>{tab.label}</Button>)}
    </div>
    {dataEntrega && <div className="line-heading">
      <span>Entregas de {formatDay(dataEntrega)}</span>
      <Button variant="ghost" onClick={() => mostrar('ABERTA')}>Ver todas em aberto</Button>
    </div>}
    {notice && <Alert tone="success">{notice}</Alert>}
    {result.loading && <LoadingState />}
    <ErrorState error={result.error} retry={result.reload} />
    {result.data?.rows.length === 0 && <EmptyState
      title={status === 'CONCLUIDA' ? 'Nenhuma encomenda concluída' : 'Nenhuma encomenda em aberto'}
      action={status === 'ABERTA' && !dataEntrega && <Button to="/pedidos/novo">Nova encomenda</Button>}>
      {status === 'CONCLUIDA' ? 'As encomendas entregues e recebidas aparecem aqui.' :
        'As encomendas com doces separados aparecem aqui.'}
    </EmptyState>}
    <div className="card-grid">
      {result.data?.rows.map((item) => <PedidoCard key={item.id} item={item}
        onAction={(concluir) => setSelected({ item, concluir })} />)}
    </div>
    {result.data && <Pagination {...result.data} onChange={setPage} />}
    {selected && <AtualizarPedido {...selected} onClose={() => setSelected(null)} onSaved={afterUpdate} />}
  </>;
}

function PedidoCard({ item, onAction }) {
  const aberto = item.status === 'ABERTA';
  return <Card as="article" className="sale-card">
    <div className="line-heading">
      <h2>{item.cliente}</h2>
      <StatusBadge tone={aberto ? 'warning' : 'success'}>{aberto ? 'Separada' : 'Concluída'}</StatusBadge>
    </div>
    <p className="field-help">Entrega em {formatDay(item.dataEntrega)}</p>
    <ul className="clean-list">
      {item.itens.map(({ produtoId, nome, quantidade }) => <li key={produtoId}>{quantidade} × {nome}</li>)}
    </ul>
    {item.observacao && <p className="order-note"><strong>Observação</strong><br />{item.observacao}</p>}
    <p className="sale-total"><span>Total</span><strong>{brl(item.valorTotal)}</strong></p>
    <div className="card-actions">
      {aberto && <>
        <Button onClick={() => onAction(true)}>Concluir e receber</Button>
        <Button variant="ghost" onClick={() => onAction(false)}>Cancelar encomenda</Button>
      </>}
      {!aberto && item.vendaId && <Button to={'/vendas/' + item.vendaId} variant="secondary">Ver venda</Button>}
    </div>
  </Card>;
}

function AtualizarPedido({ item, concluir, onClose, onSaved }) {
  const action = useAction();
  const label = concluir ? 'Concluir e receber' : 'Cancelar encomenda';
  async function salvar() {
    const operation = concluir ? concluirPedido : cancelarPedido;
    const saved = await action.run((key) => operation(item.id, key), item.id);
    if (saved) onSaved(concluir);
  }

  return <ConfirmDialog title={label} onClose={onClose} busy={action.pending} footer={<>
    <Button variant="secondary" onClick={onClose} disabled={action.pending}>Voltar</Button>
    <Button variant={concluir ? 'primary' : 'danger'} onClick={salvar} disabled={action.pending}>
      {action.pending ? 'Salvando…' : concluir ? 'Confirmar recebimento' : 'Confirmar cancelamento'}
    </Button>
  </>}>
    {concluir
      ? <p>Os doces de <strong>{item.cliente}</strong> serão marcados como entregues. Confirme se você já recebeu <strong>{brl(item.valorTotal)}</strong>.</p>
      : <p>Os doces separados para <strong>{item.cliente}</strong> ficarão disponíveis para venda. Nenhum valor será lançado.</p>}
    <ErrorState error={action.error} />
  </ConfirmDialog>;
}

export function NovaEncomenda() {
  const result = useLoad(todosProdutos, []);
  const catalogo = result.data?.filter((produto) => produto.quantidadeDisponivel > 0);
  return <>
    <PageHeader title="Nova encomenda" subtitle="Reserve doces que já estão prontos."
      back={{ to: '/pedidos', label: 'Encomendas' }} />
    {result.loading && <LoadingState />}
    <ErrorState error={result.error} retry={result.reload} />
    {catalogo?.length > 0 && <PedidoForm catalogo={catalogo} />}
    {catalogo?.length === 0 && <EmptyState title="Nenhum doce disponível"
      action={<Button to="/produtos">Ver produtos</Button>}>Produza doces antes de reservar uma encomenda.</EmptyState>}
  </>;
}

const novaLinha = () => ({ key: crypto.randomUUID(), produtoId: '', quantidade: '1' });

function PedidoForm({ catalogo }) {
  const navigate = useNavigate();
  const [cliente, setCliente] = useState('');
  const [dataEntrega, setDataEntrega] = useState(today);
  const [observacao, setObservacao] = useState('');
  const [linhas, setLinhas] = useState(() => [novaLinha()]);
  const action = useAction();
  const hoje = today();
  const items = linhas.map((linha) => {
    const produto = catalogo.find((item) => item.id === linha.produtoId);
    const quantidade = Number(linha.quantidade);
    const limite = produto ? Math.min(produto.quantidadeDisponivel, 1_000_000) : 0;
    const valida = Number.isSafeInteger(quantidade) && quantidade >= 1 && quantidade <= limite;
    return { ...linha, produto, quantidade, valida, limite };
  });
  const totalCentavos = items.reduce((total, item) =>
    item.valida ? total + cents(item.produto.precoVenda) * item.quantidade : total, 0);
  const pronto = cliente.trim() && dataEntrega >= hoje && items.every((item) => item.valida);
  const campos = action.error?.fields ?? {};

  function atualizar(key, patch) {
    action.clearError();
    setLinhas((items) => items.map((item) => item.key === key ? { ...item, ...patch } : item));
  }

  async function enviar(event) {
    event.preventDefault();
    if (!pronto) return;
    const payload = {
      cliente: cliente.trim(),
      dataEntrega,
      itens: items.map(({ produtoId, quantidade }) => ({ produtoId, quantidade })),
      observacao: observacao.trim() || null,
    };
    const saved = await action.run((key) => criarPedido(payload, key), payload);
    if (saved) navigate('/pedidos', { replace: true, state: { notice: 'Encomenda criada. Os doces foram separados.' } });
  }

  return <Card className="form-card">
    <form className="form-stack" onSubmit={enviar}>
      <div className="form-columns">
        <TextField label="Nome de quem pediu" value={cliente} maxLength={100} onChange={setCliente}
          required error={campos.cliente} />
        <TextField type="date" label="Data da entrega" value={dataEntrega} min={hoje} onChange={setDataEntrega}
          required error={campos.dataEntrega} />
      </div>
      <fieldset className="form-section">
        <legend>Doces para separar</legend>
        {items.map(({ key, produtoId, produto, valida, limite }, index) => <div className="entry-row" key={key}>
          <SelectField label={'Produto ' + (index + 1)} value={produtoId} required
            onChange={(value) => atualizar(key, { produtoId: value })}
            error={campos['itens[' + index + '].produtoId']} options={[
              { value: '', label: 'Selecione' },
              ...catalogo.filter((item) => item.id === produtoId || !linhas.some((linha) => linha.produtoId === item.id))
                .map((item) => ({ value: item.id, label: item.nome + ' · ' + brl(item.precoVenda) })),
            ]} />
          <QuantityInput label={'Quantidade do produto ' + (index + 1)} unit="un." whole
            value={linhas[index].quantidade} onChange={(value) => atualizar(key, { quantidade: value })}
            error={produto && linhas[index].quantidade && !valida ? 'Informe de 1 a ' + limite + ' unidades.' :
              campos['itens[' + index + '].quantidade']} />
          {linhas.length > 1 && <Button variant="ghost" onClick={() =>
            setLinhas((items) => items.filter((item) => item.key !== key))}>Remover</Button>}
          {produto && <p className="field-help row-help">{produto.quantidadeDisponivel} disponíveis em estoque</p>}
        </div>)}
        <Button variant="secondary" disabled={linhas.length >= Math.min(catalogo.length, 50)}
          onClick={() => setLinhas((items) => [...items, novaLinha()])}>Adicionar produto</Button>
      </fieldset>
      <TextField as="textarea" rows={3} label="Observação (opcional)" maxLength={500} value={observacao}
        onChange={setObservacao} error={campos.observacao} />
      <p className="sale-total"><span>Total da encomenda</span><strong>{brl(totalCentavos / 100)}</strong></p>
      <p className="field-help">O dinheiro entra nos recebimentos quando você concluir a encomenda.</p>
      <ErrorState error={action.error} />
      <div className="form-actions">
        <Button to="/pedidos" variant="secondary">Voltar</Button>
        <Button type="submit" disabled={!pronto || action.pending}>{action.pending ? 'Separando…' : 'Separar doces'}</Button>
      </div>
    </form>
  </Card>;
}

import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { normalizePage } from '../../api/client';
import { todosProdutos } from '../../api/produtos';
import { criarVenda, receberVenda, venda, vendas } from '../../api/vendas';
import { brl, cents, decimal, formatDay } from '../../app/format';
import { useAction, useLoad } from '../../app/hooks';
import { Alert, Button, Card, ConfirmDialog, EmptyState, ErrorState, Icon, LoadingState, MoneyInput,
  PageHeader, Pagination, QuantityInput, SelectField, StatusBadge, TextField } from '../../components/ui';

export function Vendas() {
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState(null);
  const [received, setReceived] = useState(false);
  const result = useLoad((signal) => vendas(page, signal).then(normalizePage), [page]);
  return <>
    <PageHeader title="Vendas" subtitle="Acompanhe o que vendeu e o que ainda tem a receber."
      action={<Button to="/vendas/nova"><Icon name="mais" size={19} /> Registrar venda</Button>} />
    {received && <Alert tone="success">Pagamento registrado. O estoque não mudou.</Alert>}
    {result.loading && <LoadingState />}
    <ErrorState error={result.error} retry={result.reload} />
    {result.data?.rows.length === 0 && <EmptyState title="Nenhuma venda por aqui"
      action={<Button to="/vendas/nova">Registrar venda</Button>}>Suas vendas vão aparecer aqui.</EmptyState>}
    <div className="card-grid">{result.data?.rows.map((item) => <VendaCard key={item.id} item={item} onReceive={setSelected} />)}</div>
    {result.data && <Pagination {...result.data} onChange={setPage} />}
    {selected && <Recebimento item={selected} onClose={() => setSelected(null)} onReceived={() => {
      setReceived(true); setSelected(null); result.reload();
    }} />}
  </>;
}

function VendaCard({ item, onReceive, details = false }) {
  return <Card as="article" className="sale-card">
    <div className="line-heading">
      <h2>{formatDay(item.dataVenda)}</h2>
      <StatusBadge tone={item.pagamentoPendente ? 'warning' : 'success'}>
        {item.pagamentoPendente ? 'A receber' : 'Recebida'}
      </StatusBadge>
    </div>
    <ul className="clean-list">{item.itens.map(({ produtoId, nome, quantidade }) => <li key={produtoId}>{quantidade} × {nome}</li>)}</ul>
    <p className="sale-total"><span>Total</span><strong>{brl(item.valorTotal)}</strong></p>
    {details && <div className="sale-details">
      {item.dataRecebimento
        ? <p>O valor total entrou em Recebimentos em <strong>{formatDay(item.dataRecebimento)}</strong>.</p>
        : <p>O valor aparece nos recebimentos do dia em que você marcar o pagamento como recebido.</p>}
      <p className="field-help">O custo da produção fica como referência em Produtos. Os gastos entram no painel quando você registra a compra.</p>
      {item.observacao && <p><strong>Observação</strong><br />{item.observacao}</p>}
    </div>}
    <div className="card-actions">
      {item.pagamentoPendente && <Button onClick={() => onReceive(item)}>Marcar como recebido</Button>}
      {!details && <Button variant="secondary" to={'/vendas/' + item.id}>Ver venda</Button>}
    </div>
  </Card>;
}

function Recebimento({ item, onClose, onReceived }) {
  const action = useAction();
  return <ConfirmDialog title="Marcar como recebido" onClose={onClose} busy={action.pending} footer={<>
    <Button variant="secondary" onClick={onClose} disabled={action.pending}>Voltar</Button>
    <Button disabled={action.pending} onClick={async () => {
      const result = await action.run((key) => receberVenda(item.id, key), item.id);
      if (result !== undefined) onReceived();
    }}>{action.pending ? 'Registrando…' : 'Confirmar recebimento'}</Button>
  </>}>
    <p><strong>{brl(item.valorTotal)}</strong> entram nos recebimentos de hoje. Os doces já saíram do estoque.</p>
    <ErrorState error={action.error} />
  </ConfirmDialog>;
}

export function VendaDetalhe() {
  const { id } = useParams();
  const [showDialog, setShowDialog] = useState(false);
  const [received, setReceived] = useState(false);
  const result = useLoad((signal) => venda(id, signal), [id]);
  return <>
    <PageHeader title="Detalhe da venda" back={{ to: '/vendas', label: 'Vendas' }} />
    {received && <Alert tone="success">Recebimento confirmado.</Alert>}
    {result.loading && <LoadingState />}
    <ErrorState error={result.error} retry={result.reload} />
    {result.data && <div className="detail-card"><VendaCard item={result.data} onReceive={() => setShowDialog(true)} details /></div>}
    {showDialog && result.data && <Recebimento item={result.data} onClose={() => setShowDialog(false)} onReceived={() => {
      setReceived(true); setShowDialog(false); result.reload();
    }} />}
  </>;
}

export function NovaVenda() {
  const result = useLoad(todosProdutos, []);
  const catalogo = result.data?.filter((item) => item.quantidadeDisponivel > 0);
  return <>
    <PageHeader title="Registrar venda" subtitle="Escolha os doces entregues e confira o total."
      back={{ to: '/vendas', label: 'Vendas' }} />
    {result.loading && <LoadingState />}
    <ErrorState error={result.error} retry={result.reload} />
    {catalogo?.length > 0 && <VendaForm catalogo={catalogo} />}
    {catalogo?.length === 0 && <EmptyState title="Você ainda não tem doces disponíveis"
      action={<Button to="/produtos">Ir para Produtos</Button>}>Registre uma produção antes da venda.</EmptyState>}
  </>;
}

const novaLinha = () => ({ key: crypto.randomUUID(), produtoId: '', quantidade: '1' });

function VendaForm({ catalogo }) {
  const [linhas, setLinhas] = useState(() => [novaLinha()]);
  const [editado, setEditado] = useState(false);
  const [valor, setValor] = useState('');
  const [pagamentoPendente, setPagamentoPendente] = useState(false);
  const [observacao, setObservacao] = useState('');
  const [campoErro, setCampoErro] = useState(null);
  const action = useAction();
  const navigate = useNavigate();
  const totalCentavos = linhas.reduce((total, linha) => {
    const selected = catalogo.find((item) => item.id === linha.produtoId);
    const quantidade = Number(linha.quantidade);
    return selected && Number.isInteger(quantidade) && quantidade >= 0
      ? total + cents(selected.precoVenda) * quantidade : total;
  }, 0);
  const totalSugerido = (totalCentavos / 100).toFixed(2);
  function atualizar(key, patch) {
    setCampoErro(null);
    setLinhas((items) => items.map((entry) => entry.key === key ? { ...entry, ...patch } : entry));
  }
  async function submit(event) {
    event.preventDefault();
    const index = linhas.findIndex((linha) => {
      const item = catalogo.find((entry) => entry.id === linha.produtoId);
      return !item || Number(linha.quantidade) <= 0 || Number(linha.quantidade) > item.quantidadeDisponivel;
    });
    if (index !== -1) {
      setCampoErro(index);
      return;
    }
    const payload = {
      itens: linhas.map(({ produtoId, quantidade }) => ({ produtoId, quantidade: Number(quantidade) })),
      ...(editado ? { valorTotal: Number(decimal(valor)) } : {}),
      pagamentoPendente,
      observacao: observacao.trim() || null,
    };
    const saved = await action.run((key) => criarVenda(payload, key), payload);
    if (saved) navigate('/vendas/' + saved.id, { replace: true });
  }
  return <Card className="form-card">
    <form className="form-stack" onSubmit={submit}>
      <fieldset className="form-section">
        <legend>Doces vendidos</legend>
        {linhas.map((linha, index) => {
          const selected = catalogo.find((item) => item.id === linha.produtoId);
          return <div className="entry-row" key={linha.key}>
            <SelectField label={'Produto ' + (index + 1)} value={linha.produtoId} required
              onChange={(value) => atualizar(linha.key, { produtoId: value })} options={[
                { value: '', label: 'Selecione' },
                ...catalogo.filter((item) => item.id === linha.produtoId || !linhas.some((entry) => entry.produtoId === item.id))
                  .map((item) => ({ value: item.id, label: item.nome + ' · ' + brl(item.precoVenda) })),
              ]} error={action.error?.fields['itens[' + index + '].produtoId']} />
            <QuantityInput label={'Quantidade do produto ' + (index + 1)} unit="un." whole
              value={linha.quantidade} onChange={(value) => atualizar(linha.key, { quantidade: value })}
              error={campoErro === index ? 'Informe de 1 a ' + selected?.quantidadeDisponivel + ' unidades.' :
                action.error?.fields['itens[' + index + '].quantidade']} />
            {linhas.length > 1 && <Button variant="ghost" onClick={() => {
              setCampoErro(null);
              setLinhas((items) => items.filter((entry) => entry.key !== linha.key));
            }}>Remover</Button>}
            {selected && <p className="field-help row-help">{selected.quantidadeDisponivel} disponíveis em estoque</p>}
          </div>;
        })}
        <Button variant="secondary" disabled={linhas.length >= catalogo.length || linhas.length >= 50}
          onClick={() => setLinhas((items) => [...items, novaLinha()])}>Adicionar produto</Button>
      </fieldset>
      <p className="sale-total"><span>Total sugerido</span><strong>{brl(totalSugerido)}</strong></p>
      <MoneyInput label="Valor total final" help="Você pode ajustar o total, por exemplo para dar um desconto."
        value={editado ? valor : totalSugerido.replace('.', ',')} onChange={(value) => {
          setValor(value); setEditado(true);
        }} error={action.error?.fields.valorTotal} />
      {editado && <Button variant="ghost" onClick={() => setEditado(false)}>Usar total sugerido</Button>}
      <label className="checkbox-field"><input type="checkbox" checked={pagamentoPendente}
        onChange={(event) => setPagamentoPendente(event.target.checked)} /> Pagamento pendente</label>
      <p className="field-help">{pagamentoPendente ? 'Os doces saem do estoque agora. O dinheiro entra quando você marcar o pagamento como recebido.'
        : 'O valor entra nos recebimentos assim que você confirmar.'}</p>
      <TextField as="textarea" rows={3} label="Observação (opcional)" value={observacao} maxLength={300}
        onChange={setObservacao} error={action.error?.fields.observacao} />
      <ErrorState error={action.error} />
      <div className="form-actions">
        <Button to="/vendas" variant="secondary">Cancelar</Button>
        <Button type="submit" disabled={action.pending}>{action.pending ? 'Registrando…' : 'Confirmar venda'}</Button>
      </div>
    </form>
  </Card>;
}

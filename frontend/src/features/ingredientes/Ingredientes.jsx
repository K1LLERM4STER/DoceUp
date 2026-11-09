import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { normalizePage } from '../../api/client';
import { ajustarIngrediente, comprar, ingredientes, novoIngrediente, todosIngredientes } from '../../api/ingredientes';
import { brl, decimal, inputNumber, qtd, today, unit } from '../../app/format';
import { useAction, useLoad } from '../../app/hooks';
import { Alert, Button, Card, ConfirmDialog, EmptyState, ErrorState, Icon, LoadingState,
  MoneyInput, PageHeader, Pagination, QuantityInput, SelectField, StatusBadge, TextField } from '../../components/ui';

export default function Ingredientes() {
  const [params, setParams] = useSearchParams();
  const [modal, setModal] = useState(params.get('acao') === 'compra' ? { type: 'compra' } : null);
  const [message, setMessage] = useState('');
  const [page, setPage] = useState(0);
  const result = useLoad((signal) => ingredientes(page, '', signal).then(normalizePage), [page]);
  function close() {
    setModal(null);
    if (params.has('acao')) setParams({}, { replace: true });
  }
  function saved(message) {
    close();
    setMessage(message);
    result.reload();
  }
  return <>
    <PageHeader title="Ingredientes" subtitle="Veja o que você tem para a próxima produção."
      action={<Button onClick={() => setModal({ type: 'compra' })}><Icon name="mais" size={19} /> Adicionar compra</Button>} />
    <div className="list-toolbar"><Button variant="secondary" onClick={() => setModal({ type: 'novo' })}>Novo ingrediente</Button></div>
    {message && <Alert tone="success">{message}</Alert>}
    {result.loading && <LoadingState />}
    <ErrorState error={result.error} retry={result.reload} />
    {result.data?.rows.length === 0 && <EmptyState title="Adicione um ingrediente" action={
      <Button onClick={() => setModal({ type: 'novo' })}>Novo ingrediente</Button>
    }>Depois você pode registrar a primeira compra.</EmptyState>}
    <div className="card-grid">
      {result.data?.rows.map((item) => <Card as="article" className="list-card" key={item.id}>
        <div className="line-heading"><h2>{item.nome}</h2>
          {Number(item.quantidade) < 0 && <StatusBadge tone="warning">Estoque negativo</StatusBadge>}
        </div>
        <p><strong>{qtd(item.quantidade)} {unit(item.unidade)}</strong> em estoque</p>
        {item.tipo === 'EMBALAGEM'
          ? <p className="field-help">Na produção, você informa o custo das embalagens usadas.</p>
          : <p className="field-help">{item.ultimoCustoUnitario == null ? 'Sem compra registrada' :
            'Último custo: ' + brl(item.ultimoCustoUnitario) + ' por ' + unit(item.unidade)}</p>}
        <div className="card-actions">
          <Button variant="secondary" onClick={() => setModal({ type: 'compra', item })}>Adicionar compra</Button>
          <Button variant="ghost" onClick={() => setModal({ type: 'ajuste', item })}>Ajustar estoque</Button>
        </div>
      </Card>)}
    </div>
    {result.data && <Pagination {...result.data} onChange={setPage} />}
    {modal?.type === 'novo' && <NovoIngrediente onClose={close} onSaved={() => saved('Ingrediente cadastrado.')} />}
    {modal?.type === 'compra' && <Compra onClose={close} initial={modal.item} onSaved={() => saved('Compra registrada e estoque atualizado.')} />}
    {modal?.type === 'ajuste' && <AjusteIngrediente onClose={close} item={modal.item} onSaved={() => saved('Estoque atualizado sem registrar gasto.')} />}
  </>;
}

function DialogActions({ pending, onClose, label }) {
  return <div className="dialog-actions">
    <Button variant="secondary" disabled={pending} onClick={onClose}>Cancelar</Button>
    <Button type="submit" disabled={pending}>{pending ? 'Salvando…' : label}</Button>
  </div>;
}

function NovoIngrediente({ onClose, onSaved }) {
  const [nome, setNome] = useState('');
  const [unidade, setUnidade] = useState('KG');
  const action = useAction();
  const options = [{ value: 'KG', label: 'Quilo (kg)' }, { value: 'L', label: 'Litro (L)' },
    { value: 'UN', label: 'Unidade (un.)' }, { value: 'PACOTE', label: 'Pacote' }];
  return <ConfirmDialog title="Novo ingrediente" onClose={onClose} busy={action.pending}>
    <form className="form-stack" onSubmit={async (event) => {
      event.preventDefault();
      const data = { nome: nome.trim(), unidade, tipo: 'INGREDIENTE' };
      const result = await action.run((key) => novoIngrediente(data, key), data);
      if (result !== undefined) onSaved();
    }}>
      <TextField label="Nome" value={nome} onChange={setNome} maxLength={100} required error={action.error?.fields.nome} />
      <SelectField label="Unidade" value={unidade} onChange={setUnidade} options={options} />
      <ErrorState error={action.error} />
      <DialogActions pending={action.pending} onClose={onClose} label="Salvar ingrediente" />
    </form>
  </ConfirmDialog>;
}

function Compra({ initial, onClose, onSaved }) {
  const result = useLoad((signal) => initial ? Promise.resolve([initial]) : todosIngredientes(signal), [initial]);
  const [id, setId] = useState(initial?.id || '');
  const [quantidade, setQuantidade] = useState('');
  const [valorTotal, setValorTotal] = useState('');
  const [data, setData] = useState(today);
  const action = useAction();
  const selected = result.data?.find((item) => item.id === id);
  return <ConfirmDialog title="Adicionar compra" onClose={onClose} busy={action.pending}>
    {result.loading && <LoadingState />}
    <ErrorState error={result.error} retry={result.reload} />
    {result.data && <form className="form-stack" onSubmit={async (event) => {
      event.preventDefault();
      const payload = { quantidade: Number(decimal(quantidade)), valorTotal: Number(decimal(valorTotal)), data };
      const saved = await action.run((key) => comprar(id, payload, key), [id, payload]);
      if (saved !== undefined) onSaved();
    }}>
      <p>O valor total entra em Gastos no dia informado. Se você trouxe algo de casa, pode usar valor zero.</p>
      <SelectField label="Ingrediente ou embalagem" value={id} onChange={setId} required options={[
        { value: '', label: 'Selecione' },
        ...result.data.map((item) => ({ value: item.id, label: item.nome })),
      ]} />
      <QuantityInput label="Quantidade comprada" unit={unit(selected?.unidade)} value={quantidade} onChange={setQuantidade}
        error={action.error?.fields.quantidade} />
      <MoneyInput label="Valor total pago" value={valorTotal} onChange={setValorTotal} error={action.error?.fields.valorTotal} />
      <TextField label="Data da compra" type="date" max={today()} value={data} onChange={setData} required error={action.error?.fields.data} />
      <ErrorState error={action.error} />
      <DialogActions pending={action.pending} onClose={onClose} label="Registrar compra" />
    </form>}
  </ConfirmDialog>;
}

export function AjusteIngrediente({ item, onClose, onSaved }) {
  const [quantidade, setQuantidade] = useState(inputNumber(Math.max(0, Number(item.quantidade))));
  const [motivo, setMotivo] = useState('');
  const action = useAction();
  return <ConfirmDialog title={'Ajustar estoque de ' + item.nome} onClose={onClose} busy={action.pending}>
    <form className="form-stack" onSubmit={async (event) => {
      event.preventDefault();
      const data = { quantidade: Number(decimal(quantidade)), motivo: motivo.trim() || null };
      const result = await action.run((key) => ajustarIngrediente(item.id, data, key), data);
      if (result !== undefined) onSaved();
    }}>
      <p>Informe quanto há agora. Este ajuste não altera os gastos.</p>
      <QuantityInput label="Novo total" unit={unit(item.unidade)} value={quantidade} onChange={setQuantidade}
        error={action.error?.fields.quantidade} />
      <TextField label="Motivo (opcional)" value={motivo} onChange={setMotivo} maxLength={200} error={action.error?.fields.motivo} />
      <ErrorState error={action.error} />
      <DialogActions pending={action.pending} onClose={onClose} label="Salvar ajuste" />
    </form>
  </ConfirmDialog>;
}

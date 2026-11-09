import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { normalizePage } from '../../api/client';
import { ajustarProduto, produto, produtos, salvarFoto, salvarProduto } from '../../api/produtos';
import { brl, decimal, inputAmount } from '../../app/format';
import { useAction, useLoad } from '../../app/hooks';
import { Alert, Button, Card, ConfirmDialog, EmptyState, ErrorState, FormField, Icon,
  LoadingState, MoneyInput, PageHeader, Pagination, QuantityInput, TextField } from '../../components/ui';

export function Produtos() {
  const [busca, setBusca] = useState('');
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState(null);
  const [message, setMessage] = useState('');
  const result = useLoad((signal) => produtos(page, busca, signal).then(normalizePage), [page, busca]);
  return <>
    <PageHeader title="Produtos" subtitle="Cuide dos doces prontos e planeje a próxima produção."
      action={<Button to="/produtos/novo"><Icon name="mais" size={19} /> Novo produto</Button>} />
    <div className="list-toolbar">
      <TextField label="Buscar produto" type="search" value={busca} maxLength={100}
        onChange={(value) => { setBusca(value); setPage(0); }} />
    </div>
    {message && <Alert tone="success">{message}</Alert>}
    {result.loading && <LoadingState />}
    <ErrorState error={result.error} retry={result.reload} />
    {result.data?.rows.length === 0 && <EmptyState title={busca ? 'Nenhum doce encontrado' : 'Cadastre seu primeiro doce'}
      action={!busca && <Button to="/produtos/novo">Novo produto</Button>}>
      {busca ? 'Tente buscar por outro nome.' : 'Adicione um produto para registrar produções e vendas.'}
    </EmptyState>}
    <div className="card-grid">
      {result.data?.rows.map((item) => <Card as="article" className="product-card" key={item.id}>
        <div className="product-summary">
          {item.fotoUrl
            ? <img className="product-photo" src={item.fotoUrl} alt={item.nome} loading="lazy" />
            : <img className="product-photo" src="/doce-padrao.svg" alt="" loading="lazy" />}
          <div>
            <h2>{item.nome}</h2>
            <p>{item.quantidadeDisponivel} disponíveis · {item.quantidadeReservada} separados</p>
            <p>Venda: <strong>{brl(item.precoVenda)}</strong></p>
            <p>{item.custoUnitario == null ? 'Custo ainda não calculado' : <>Custo por doce: <strong>{brl(item.custoUnitario)}</strong></>}</p>
            {item.custoUnitario != null && <p className="field-help">Referência da última produção</p>}
          </div>
        </div>
        <div className="card-actions">
          <Button to={'/producao/' + item.id}>Produzir</Button>
          <Button variant="secondary" onClick={() => setSelected(item)}>Ajustar estoque</Button>
          <Button variant="ghost" to={'/produtos/' + item.id + '/editar'}>Editar</Button>
        </div>
      </Card>)}
    </div>
    {result.data && <Pagination {...result.data} onChange={setPage} />}
    {selected && <AjusteProduto item={selected} onClose={() => setSelected(null)} onSaved={() => {
      setSelected(null);
      setMessage('Estoque de doces atualizado.');
      result.reload();
    }} />}
  </>;
}

function AjusteProduto({ item, onClose, onSaved }) {
  const [quantidade, setQuantidade] = useState(String(item.quantidadeTotal));
  const [motivo, setMotivo] = useState('');
  const [error, setError] = useState(null);
  const action = useAction();
  async function submit(event) {
    event.preventDefault();
    if (Number(quantidade) < item.quantidadeReservada) {
      setError('O total precisa incluir os doces separados para encomendas.');
      return;
    }
    const data = { quantidade: Number(quantidade), motivo: motivo.trim() || null };
    const result = await action.run((key) => ajustarProduto(item.id, data, key), data);
    if (result !== undefined) onSaved();
  }
  return <ConfirmDialog title={'Ajustar estoque de ' + item.nome} onClose={onClose} busy={action.pending}>
    <form className="form-stack" onSubmit={submit}>
      <p>Informe o novo total de doces em mãos, incluindo os separados. Este ajuste não muda o saldo em dinheiro.</p>
      <QuantityInput label="Novo total" unit="un." whole value={quantidade} onChange={(value) => {
        setQuantidade(value); setError(null);
      }} error={error || action.error?.fields.quantidade} />
      <TextField label="Motivo (opcional)" value={motivo} onChange={setMotivo} maxLength={200} error={action.error?.fields.motivo} />
      <ErrorState error={action.error} />
      <div className="dialog-actions">
        <Button variant="secondary" onClick={onClose} disabled={action.pending}>Cancelar</Button>
        <Button type="submit" disabled={action.pending}>{action.pending ? 'Salvando…' : 'Salvar ajuste'}</Button>
      </div>
    </form>
  </ConfirmDialog>;
}

export function ProdutoForm() {
  const { id } = useParams();
  const result = useLoad((signal) => id ? produto(id, signal) : Promise.resolve(null), [id]);
  return <>
    <PageHeader title={id ? 'Editar produto' : 'Novo produto'} back={{ to: '/produtos', label: 'Produtos' }} />
    {result.loading && <LoadingState />}
    <ErrorState error={result.error} retry={result.reload} />
    {result.data !== undefined && <ProdutoFields key={id || 'novo'} item={result.data} />}
  </>;
}

function ProdutoFields({ item }) {
  const [nome, setNome] = useState(item?.nome || '');
  const [precoVenda, setPrecoVenda] = useState(inputAmount(item?.precoVenda));
  const [file, setFile] = useState(null);
  const [savedId, setSavedId] = useState(item?.id);
  const [fotoError, setFotoError] = useState(null);
  const action = useAction();
  const navigate = useNavigate();
  async function submit(event) {
    event.preventDefault();
    setFotoError(null);
    const data = { nome: nome.trim(), precoVenda: Number(decimal(precoVenda)) };
    if (file && file.size > 1024 * 1024) {
      setFotoError('Escolha uma imagem de até 1 MB.');
      return;
    }
    const saved = await action.run(async (key) => {
      const updated = await salvarProduto(savedId, data, key);
      setSavedId(updated.id);
      if (file) {
        try {
          await salvarFoto(updated.id, file);
        } catch (error) {
          setFotoError('Os dados do produto foram salvos. A foto não foi enviada. ' + error.message);
          return null;
        }
      }
      return updated;
    }, [savedId, data]);
    if (saved) navigate('/produtos', { replace: true });
  }
  return <Card className="form-card">
    <form className="form-stack" onSubmit={submit}>
      <TextField label="Nome do doce" value={nome} onChange={setNome} maxLength={100} required error={action.error?.fields.nome} />
      <MoneyInput label="Preço de venda" value={precoVenda} onChange={setPrecoVenda} error={action.error?.fields.precoVenda} />
      <FormField label="Foto (opcional)" help="JPG ou PNG, até 1 MB. Sem foto, usamos a imagem padrão." error={fotoError}>
        {(attributes) => <input {...attributes} type="file" accept="image/jpeg,image/png"
          onChange={(event) => { setFile(event.target.files[0] || null); setFotoError(null); }} />}
      </FormField>
      <ErrorState error={action.error} />
      <div className="form-actions">
        <Button to="/produtos" variant="secondary">Cancelar</Button>
        <Button type="submit" disabled={action.pending}>{action.pending ? 'Salvando…' : 'Salvar produto'}</Button>
      </div>
    </form>
  </Card>;
}

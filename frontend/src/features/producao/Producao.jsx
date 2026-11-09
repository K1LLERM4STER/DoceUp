import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { todosIngredientes } from '../../api/ingredientes';
import { confirmarProducao, previaProducao, ultimaProducao } from '../../api/producao';
import { produto } from '../../api/produtos';
import { brl, decimal, inputAmount, inputNumber, qtd, unit } from '../../app/format';
import { useAction, useLoad } from '../../app/hooks';
import { Alert, Button, Card, ConfirmDialog, EmptyState, ErrorState, LoadingState, MoneyInput,
  PageHeader, QuantityInput, SelectField } from '../../components/ui';
import { AjusteIngrediente } from '../ingredientes/Ingredientes';

const newIngredient = () => ({ key: crypto.randomUUID(), insumoId: '', quantidade: '' });

function roundQuantity(value) {
  return value > 0 ? Math.max(0.001, Number(value.toFixed(3))) : 0;
}

function usePreview(id, signature) {
  const [state, setState] = useState({});
  const [revision, setRevision] = useState(0);
  useEffect(() => {
    if (!signature) return;
    const controller = new AbortController();
    const timer = window.setTimeout(async () => {
      try {
        const data = await previaProducao(id, JSON.parse(signature), controller.signal);
        if (!controller.signal.aborted) setState({ signature, revision, data });
      } catch (error) {
        if (!controller.signal.aborted) setState({ signature, revision, error });
      }
    }, 220);
    return () => {
      window.clearTimeout(timer);
      controller.abort();
    };
  }, [id, signature, revision]);
  const current = state.signature === signature && state.revision === revision;
  return { ...(current ? state : { loading: Boolean(signature) }), retry: () => setRevision((value) => value + 1) };
}

export default function Producao() {
  const { produtoId } = useParams();
  const result = useLoad((signal) => Promise.all([
    produto(produtoId, signal), todosIngredientes(signal), ultimaProducao(produtoId, signal),
  ]), [produtoId]);
  const [item, insumos, ultima] = result.data || [];
  return <>
    <PageHeader title={item ? 'Produzir ' + item.nome : 'Produção'}
      subtitle="Registre os ingredientes usados e quantos doces ficaram prontos."
      back={{ to: '/produtos', label: 'Produtos' }} />
    {result.loading && <LoadingState />}
    <ErrorState error={result.error} retry={result.reload} />
    {item && <ProducaoForm key={item.id} item={item} insumos={insumos} ultima={ultima} />}
  </>;
}

function ProducaoForm({ item, insumos, ultima }) {
  const [rendimento, setRendimento] = useState(inputNumber(ultima?.rendimento));
  const [linhas, setLinhas] = useState(() => ultima?.ingredientes.length
    ? ultima.ingredientes.map((entry) => ({ ...entry, key: crypto.randomUUID(), quantidade: inputNumber(entry.quantidade) }))
    : [newIngredient()]);
  const [proporcional, setProporcional] = useState(Boolean(ultima));
  const [temEmbalagem, setTemEmbalagem] = useState(Boolean(ultima?.quantidadeEmbalagem));
  const [quantidadeEmbalagem, setQuantidadeEmbalagem] = useState(inputNumber(ultima?.quantidadeEmbalagem));
  const [custoEmbalagem, setCustoEmbalagem] = useState(inputAmount(ultima?.custoEmbalagem));
  const [dialog, setDialog] = useState(false);
  const [ajuste, setAjuste] = useState(null);
  const [ajustado, setAjustado] = useState(false);
  const [saved, setSaved] = useState(null);
  const action = useAction();
  const catalogo = insumos.filter((entry) => entry.tipo !== 'EMBALAGEM');

  const rendimentoNum = Number(rendimento);
  const linhasValidas = linhas.length && linhas.every((entry) =>
    entry.insumoId && /^\d+(?:[.,]\d{1,3})?$/.test(entry.quantidade) && Number(decimal(entry.quantidade)) > 0);
  const embalagemValida = !temEmbalagem || (
    /^\d+(?:[.,]\d{1,3})?$/.test(quantidadeEmbalagem) && Number(decimal(quantidadeEmbalagem)) > 0
    && /^\d+(?:[.,]\d{1,2})?$/.test(custoEmbalagem));
  const payload = linhasValidas && embalagemValida && /^\d+$/.test(rendimento) && rendimentoNum > 0
    ? {
        rendimento: rendimentoNum,
        ingredientes: linhas.map(({ insumoId, quantidade }) => ({ insumoId, quantidade: Number(decimal(quantidade)) })),
        quantidadeEmbalagem: temEmbalagem ? Number(decimal(quantidadeEmbalagem)) : 0,
        custoEmbalagem: temEmbalagem ? Number(decimal(custoEmbalagem)) : 0,
        aceitarEstoqueNegativo: false,
      }
    : null;
  const preview = usePreview(item.id, !saved && payload ? JSON.stringify(payload) : null);
  const faltas = action.error?.faltas || preview.data?.faltas || [];
  function usarBase(valor) {
    if (!ultima || !Number(valor)) return;
    const factor = Number(valor) / ultima.rendimento;
    setLinhas(ultima.ingredientes.map((entry) => ({
      insumoId: entry.insumoId, key: crypto.randomUUID(), quantidade: inputNumber(roundQuantity(entry.quantidade * factor)),
    })));
    setTemEmbalagem(Boolean(ultima.quantidadeEmbalagem));
    setQuantidadeEmbalagem(inputNumber(roundQuantity((ultima.quantidadeEmbalagem || 0) * factor)));
    setCustoEmbalagem(inputAmount((ultima.custoEmbalagem || 0) * factor));
  }
  function alterarLinha(key, patch) {
    setProporcional(false);
    action.clearError();
    setLinhas((items) => items.map((entry) => entry.key === key ? { ...entry, ...patch } : entry));
  }
  async function confirmar() {
    const body = { ...payload, aceitarEstoqueNegativo: faltas.length > 0 };
    const result = await action.run((key) => confirmarProducao(item.id, body, key), body);
    if (result) {
      setSaved(result);
      setDialog(false);
    }
  }
  if (saved) return <Card className="form-card">
    <Alert tone="success" title="Produção confirmada">
      <p>{saved.rendimento} doces foram adicionados ao estoque. Custo por doce: {brl(saved.custoUnitario)}.</p>
    </Alert>
    <div className="form-actions"><Button to="/produtos">Voltar aos produtos</Button></div>
  </Card>;
  if (!catalogo.length) return <EmptyState title="Adicione um ingrediente primeiro" action={<Button to="/ingredientes">Ir para Ingredientes</Button>}>
    Assim você poderá registrar as quantidades usadas na produção.
  </EmptyState>;
  return <Card className="form-card">
    <form className="form-stack" onSubmit={(event) => {
      event.preventDefault();
      if (payload && preview.data) setDialog(true);
    }}>
      {ultima && <p className="field-help">A última produção rendeu {ultima.rendimento} doces. As sugestões podem ser editadas.</p>}
      <QuantityInput label="Quantos doces você produziu?" value={rendimento} unit="un." whole
        error={action.error?.fields.rendimento} onChange={(value) => {
          setRendimento(value);
          action.clearError();
          if (proporcional && /^\d+$/.test(value)) usarBase(value);
        }} />
      {!proporcional && ultima && <Button variant="secondary" onClick={() => {
        setProporcional(true); usarBase(rendimento);
      }}>Recalcular pela última produção</Button>}
      <fieldset className="form-section">
        <legend>Ingredientes</legend>
        {linhas.map((linha, index) => {
          const selected = catalogo.find((entry) => entry.id === linha.insumoId);
          return <div className="entry-row" key={linha.key}>
            <SelectField label={'Ingrediente ' + (index + 1)} value={linha.insumoId}
              onChange={(value) => alterarLinha(linha.key, { insumoId: value })} required options={[
                { value: '', label: 'Selecione' },
                ...catalogo.filter((entry) => entry.id === linha.insumoId ||
                  !linhas.some((other) => other.insumoId === entry.id)).map((entry) => ({ value: entry.id, label: entry.nome })),
              ]} error={action.error?.fields['ingredientes[' + index + '].insumoId']} />
            <QuantityInput label={'Quantidade do ingrediente ' + (index + 1)} unit={unit(selected?.unidade)}
              value={linha.quantidade} onChange={(value) => alterarLinha(linha.key, { quantidade: value })}
              error={action.error?.fields['ingredientes[' + index + '].quantidade']} />
            {linhas.length > 1 && <Button variant="ghost" onClick={() => {
              setProporcional(false);
              action.clearError();
              setLinhas((items) => items.filter((entry) => entry.key !== linha.key));
            }}>Remover</Button>}
          </div>;
        })}
        <Button variant="secondary" disabled={linhas.length >= catalogo.length} onClick={() => {
          setProporcional(false);
          setLinhas((items) => [...items, newIngredient()]);
        }}>Adicionar ingrediente</Button>
      </fieldset>
      <fieldset className="form-section">
        <legend>Embalagem</legend>
        <label className="checkbox-field"><input type="checkbox" checked={temEmbalagem} onChange={(event) => {
          setTemEmbalagem(event.target.checked); setProporcional(false); action.clearError();
        }} /> Tem embalagem?</label>
        {temEmbalagem && <div className="form-columns">
          <QuantityInput label="Quantidade usada" unit="un." value={quantidadeEmbalagem} onChange={(value) => {
            setQuantidadeEmbalagem(value); setProporcional(false); action.clearError();
          }} error={action.error?.fields.quantidadeEmbalagem} />
          <MoneyInput label="Custo total das embalagens" value={custoEmbalagem} onChange={(value) => {
            setCustoEmbalagem(value); setProporcional(false); action.clearError();
          }} error={action.error?.fields.custoEmbalagem} />
        </div>}
      </fieldset>
      {preview.loading && <p role="status" className="field-help">Calculando o custo…</p>}
      {ajustado && <Alert tone="success">Estoque ajustado. Confira a produção atualizada.</Alert>}
      <ErrorState error={preview.error} retry={preview.retry} />
      {faltas.length > 0 && <Alert title="Faltam itens no estoque">
        <ul>{faltas.map((falta) => <li key={falta.insumoId}>
          <p>{falta.nome}: você tem {qtd(falta.disponivel)} {unit(falta.unidade)}, precisa de {qtd(falta.necessario)} {unit(falta.unidade)} e faltam {qtd(falta.falta)} {unit(falta.unidade)}.</p>
          <Button variant="secondary" aria-label={'Ajustar estoque de ' + falta.nome} onClick={() => setAjuste({
            id: falta.insumoId, nome: falta.nome, unidade: falta.unidade, quantidade: falta.disponivel,
          })}>Ajustar estoque</Button>
        </li>)}</ul>
      </Alert>}
      {preview.data && <Resumo data={preview.data} />}
      <ErrorState error={action.error} />
      <div className="form-actions">
        <Button to="/produtos" variant="secondary">Cancelar</Button>
        <Button type="submit" variant={faltas.length ? 'secondary' : 'primary'}
          disabled={!payload || !preview.data || action.pending}>
          {faltas.length ? 'Produzir mesmo faltando' : 'Confirmar produção'}
        </Button>
      </div>
    </form>
    {ajuste && <AjusteIngrediente item={ajuste} onClose={() => setAjuste(null)} onSaved={() => {
      setAjuste(null);
      setAjustado(true);
      action.clearError();
      preview.retry();
    }} />}
    {dialog && <ConfirmDialog title={faltas.length ? 'Produzir mesmo faltando?' : 'Confirmar produção'} busy={action.pending}
      onClose={() => setDialog(false)} footer={<>
        <Button variant="secondary" onClick={() => setDialog(false)} disabled={action.pending}>Voltar</Button>
        <Button onClick={confirmar} disabled={action.pending}>{action.pending ? 'Salvando…' : 'Confirmar produção'}</Button>
      </>}>
      <p>Vamos adicionar {rendimento} doces e descontar os ingredientes usados.</p>
      {faltas.length > 0 && <Alert>Os itens que estão faltando ficarão com estoque negativo.</Alert>}
      {preview.data && <Resumo data={preview.data} />}
      <ErrorState error={action.error} />
    </ConfirmDialog>}
  </Card>;
}

function Resumo({ data }) {
  return <dl className="cost-summary">
    <div><dt>Ingredientes</dt><dd>{brl(data.custoIngredientes)}</dd></div>
    <div><dt>Embalagens</dt><dd>{brl(data.custoEmbalagem)}</dd></div>
    <div><dt>Custo da produção</dt><dd>{brl(data.custoTotal)}</dd></div>
    <div><dt>Custo por doce</dt><dd>{brl(data.custoUnitario)}</dd></div>
  </dl>;
}

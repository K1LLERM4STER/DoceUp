import { useEffect, useId, useRef } from 'react';
import { Link } from 'react-router-dom';
import { brl } from '../app/format';

const icons = {
  inicio: <><path d="m3 10 9-7 9 7v10H3Z"/><path d="M9 20v-6h6v6"/></>,
  produtos: <><path d="m3 7 9-4 9 4v10l-9 4-9-4Z"/><path d="m3 7 9 4 9-4m-9 4v10"/></>,
  ingredientes: <><path d="M3 12h18a9 9 0 0 1-18 0Zm4 9h10"/><path d="m14 12 5-8a2 2 0 0 1 3 2l-4 6"/></>,
  pedidos: <><rect x="3" y="6" width="18" height="15" rx="2"/><path d="M7 3v6m10-6v6M3 11h18"/></>,
  vendas: <><path d="M4 20h16M7 16v-5m5 5V7m5 9V4"/></>,
  producao: <><path d="m13 11 6-7a2 2 0 0 1 3 3l-7 6m-2-2c-3-2-7 0-9 3s-2 5 0 6 5 0 8-2 5-5 3-7Z"/><path d="m5 18 8-7"/></>,
  carteira: <><path d="M20 8V5H6a3 3 0 0 0 0 6h15v9H6a3 3 0 0 1-3-3V8"/><path d="M21 13h-5a2 2 0 0 0 0 4h5"/></>,
  direita: <path d="m9 6 6 6-6 6"/>,
  esquerda: <path d="m15 6-6 6 6 6"/>,
  sair: <><path d="M12 4H5v16h7m2-12 5 4-5 4m5-4H9"/></>,
  marca: <><path d="M6 12h12l-1.5 8h-9Z"/><path d="M6 12c-4-1-2-5 1-5 0-3 5-4 6-1 3-1 5 0 5 3 2 0 3 3 0 3Z"/></>,
  alerta: <><path d="m12 3 9 17H3Z"/><path d="M12 9v4m0 3h.01"/></>,
  mais: <path d="M5 12h14m-7-7v14"/>,
};

export function Icon({ name, size = 22 }) {
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor"
    strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{icons[name]}</svg>;
}

export function Button({ variant = 'primary', to, children, className = '', type = 'button', ...props }) {
  const classes = 'button button-' + variant + ' ' + className;
  return to
    ? <Link className={classes} to={to} {...props}>{children}</Link>
    : <button type={type} className={classes} {...props}>{children}</button>;
}

export function PageHeader({ title, subtitle, action, back }) {
  return <>
    {back && <Link to={back.to} className="back-link"><Icon name="esquerda" size={17} />{back.label}</Link>}
    <header className="page-header">
      <div><h1>{title}</h1>{subtitle && <p>{subtitle}</p>}</div>
      {action}
    </header>
  </>;
}

export function Card({ children, className = '', as: Tag = 'div' }) {
  return <Tag className={'card ' + className}>{children}</Tag>;
}

export function StatCard({ label, value, featured, explanation }) {
  return <Card className={featured ? 'stat-card stat-featured' : 'stat-card'}>
    {featured && <Icon name="carteira" size={28} />}
    <div><span className="stat-label">{label}</span><strong>{brl(value)}</strong>
      {explanation && <span className="stat-caption">{explanation}</span>}</div>
  </Card>;
}

export function FormField({ label, help, error, children }) {
  const id = useId();
  const description = [help && id + '-help', error && id + '-error'].filter(Boolean).join(' ');
  return <div className="form-field">
    <label htmlFor={id}>{label}</label>
    {children({ id, 'aria-invalid': error ? true : undefined, 'aria-describedby': description || undefined })}
    {help && <p id={id + '-help'} className="field-help">{help}</p>}
    {error && <p id={id + '-error'} className="field-error">{error}</p>}
  </div>;
}

export function TextField({ label, help, error, value, onChange, as: Tag = 'input', ...props }) {
  return <FormField label={label} help={help} error={error}>
    {(attributes) => <Tag {...attributes} value={value} onChange={(event) => onChange(event.target.value)} {...props} />}
  </FormField>;
}

export function SelectField({ label, options, ...props }) {
  return <TextField label={label} as="select" {...props}>
    {options.map(({ value, label: text }) => <option key={value} value={value}>{text}</option>)}
  </TextField>;
}

export function MoneyInput({ label, help, error, value, onChange, required = true }) {
  return <FormField label={label} help={help} error={error}>
    {(attrs) => <div className="input-with-unit"><span aria-hidden="true">R$</span>
      <input {...attrs} value={value} onChange={(event) => onChange(event.target.value)}
        inputMode="decimal" pattern="[0-9]+([.,][0-9]{1,2})?" required={required} maxLength={14} />
    </div>}
  </FormField>;
}

export function QuantityInput({ label, unit, error, value, onChange, whole = false, signed = false, required = true }) {
  const pattern = (signed ? '-?' : '') + '[0-9]+' + (whole ? '' : '([.,][0-9]{1,3})?');
  return <FormField label={label} error={error}>
    {(attrs) => <div className="input-with-unit">
      <input {...attrs} value={value} onChange={(event) => onChange(event.target.value)}
        inputMode={whole ? 'numeric' : 'decimal'} pattern={pattern} required={required} maxLength={15} />
      {unit && <span>{unit}</span>}
    </div>}
  </FormField>;
}

export function StatusBadge({ children, tone = 'neutral' }) {
  return <span className={'badge badge-' + tone}>{children}</span>;
}

export function Alert({ title, children, tone = 'warning' }) {
  return <div className={'alert alert-' + tone} role={tone === 'error' ? 'alert' : 'status'}>
    {tone !== 'success' && <Icon name="alerta" size={19} />}
    <div>{title && <strong>{title}</strong>}{children}</div>
  </div>;
}

export function ErrorState({ error, retry }) {
  if (!error) return null;
  return <Alert tone="error" title="Não foi possível continuar">
    <p>{error.message}</p>
    {retry && <Button onClick={retry} variant="secondary">Tentar novamente</Button>}
  </Alert>;
}

export function EmptyState({ title, children, action }) {
  return <Card className="empty-state"><Icon name="produtos" size={30} /><h2>{title}</h2>
    {children && <p>{children}</p>}{action}</Card>;
}

export function LoadingState({ children = 'Carregando…' }) {
  return <Card className="loading-state"><p role="status">{children}</p></Card>;
}

export function ConfirmDialog({ title, children, onClose, footer, busy }) {
  const dialog = useRef(null);
  const id = useId();
  useEffect(() => {
    const node = dialog.current;
    const previous = document.activeElement;
    node.showModal();
    return () => {
      node.close();
      if (previous?.isConnected) previous.focus();
    };
  }, []);
  return <dialog ref={dialog} aria-labelledby={id} onCancel={(event) => {
    event.preventDefault();
    if (!busy) onClose();
  }}>
    <h2 id={id}>{title}</h2>
    <div className="dialog-body">{children}</div>
    {footer && <div className="dialog-actions">{footer}</div>}
  </dialog>;
}

export function Pagination({ page, totalPages, onChange }) {
  if (totalPages <= 1) return null;
  return <nav className="pagination" aria-label="Páginas dos resultados">
    <Button variant="secondary" aria-label="Página anterior" disabled={page <= 0} onClick={() => onChange(page - 1)}>
      <Icon name="esquerda" size={18} />
    </Button>
    <span>Página {page + 1} de {totalPages}</span>
    <Button variant="secondary" aria-label="Próxima página" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>
      <Icon name="direita" size={18} />
    </Button>
  </nav>;
}

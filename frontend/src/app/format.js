const saoPaulo = 'America/Sao_Paulo';
const money = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const quantity = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 6 });
const day = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeZone: saoPaulo });
const month = new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric', timeZone: saoPaulo });

export const brl = (value) => money.format(value ?? 0);
export const qtd = (value) => quantity.format(value ?? 0);
export const inputNumber = (value) => value == null ? '' : String(value).replace('.', ',');
export const inputAmount = (value) => value == null ? '' : Number(value).toFixed(2).replace('.', ',');
export const decimal = (value) => String(value).trim().replace(',', '.');
export const formatDay = (value) => day.format(new Date(value.length === 10 ? value + 'T12:00:00Z' : value));
export const formatMonth = (value) => month.format(new Date(value + '-01T12:00:00Z'));
export const today = () => new Intl.DateTimeFormat('sv-SE', { timeZone: saoPaulo }).format(new Date());
export const unit = (value) => ({ KG: 'kg', L: 'L', UN: 'un.', PACOTE: 'pct.' }[value] || value);

export function shiftMonth(value, amount) {
  const [year, monthIndex] = value.split('-').map(Number);
  return new Date(Date.UTC(year, monthIndex - 1 + amount, 1)).toISOString().slice(0, 7);
}

export function cents(value) {
  const normalized = decimal(value);
  const [whole, fraction = ''] = normalized.split('.');
  return Number(whole) * 100 + Number(fraction.padEnd(2, '0').slice(0, 2));
}

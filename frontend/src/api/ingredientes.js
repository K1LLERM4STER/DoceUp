import { allPages, pageParams, read, write } from './client';

export const ingredientes = (page, busca, signal) =>
  read('/ingredientes' + pageParams(page, busca), { signal });
export const todosIngredientes = (signal) => allPages('/ingredientes', signal);
export const novoIngrediente = (dados, key) => write('/ingredientes', 'POST', dados, { key });
export const comprar = (id, dados, key) => write('/ingredientes/' + id + '/compras', 'POST', dados, { key });
export const ajustarIngrediente = (id, dados, key) =>
  write('/ingredientes/' + id + '/ajustes', 'POST', dados, { key });

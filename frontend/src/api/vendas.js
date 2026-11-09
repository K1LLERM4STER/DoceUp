import { pageParams, read, write } from './client';

export const vendas = (page, signal) => read('/vendas' + pageParams(page), { signal });
export const venda = (id, signal) => read('/vendas/' + id, { signal });
export const criarVenda = (dados, key) => write('/vendas', 'POST', dados, { key });
export const receberVenda = (id, key) => write('/vendas/' + id + '/recebimento', 'POST', undefined, { key });

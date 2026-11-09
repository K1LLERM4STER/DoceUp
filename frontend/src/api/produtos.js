import { allPages, pageParams, read, write } from './client';

export const produtos = (page, busca, signal) => read('/produtos' + pageParams(page, busca), { signal });
export const todosProdutos = (signal) => allPages('/produtos', signal);
export const produto = (id, signal) => read('/produtos/' + id, { signal });
export const salvarProduto = (id, dados, key) =>
  write(id ? '/produtos/' + id : '/produtos', id ? 'PUT' : 'POST', dados, { key });
export const ajustarProduto = (id, dados, key) => write('/produtos/' + id + '/ajustes', 'POST', dados, { key });
export function salvarFoto(id, file) {
  const body = new FormData();
  body.set('file', file);
  return write('/produtos/' + id + '/foto', 'PUT', body);
}

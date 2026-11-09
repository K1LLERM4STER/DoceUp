import { read, write } from './client';

export function pedidos(page, status, dataEntrega, signal) {
  const params = new URLSearchParams({ page, status });
  if (dataEntrega) params.set('dataEntrega', dataEntrega);
  return read('/pedidos?' + params, { signal });
}

export const criarPedido = (data, key) => write('/pedidos', 'POST', data, { key });
export const concluirPedido = (id, key) => write('/pedidos/' + id + '/conclusao', 'POST', undefined, { key });
export const cancelarPedido = (id, key) => write('/pedidos/' + id + '/cancelamento', 'POST', undefined, { key });

import { read, write } from './client';

export const ultimaProducao = (id, signal) => read('/producao/' + id + '/ultima', { signal });
export const previaProducao = (id, dados, signal) => write('/producao/' + id + '/previa', 'POST', dados, { signal });
export const confirmarProducao = (id, dados, key) => write('/producao/' + id, 'POST', dados, { key });

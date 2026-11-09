import { read } from './client';

export const painel = (filter, signal) => read('/painel?' + new URLSearchParams(filter), { signal });

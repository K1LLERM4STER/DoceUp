import { read } from './client';

export const me = (signal) => read('/auth/me', { signal, handleUnauthorized: false });
export const googleLogin = '/oauth2/authorization/google';

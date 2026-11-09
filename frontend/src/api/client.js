export class ApiError extends Error {
  constructor(status, problem) {
    super(problem?.detail || problem?.title || 'Não deu para completar a ação. Tente de novo.');
    this.status = status;
    this.code = problem?.codigo ?? problem?.code;
    this.faltas = problem?.faltas;
    const fields = problem?.campos ?? problem?.errors ?? problem?.fieldErrors;
    this.fields = Array.isArray(fields)
      ? Object.fromEntries(fields.map(({ field, message }) => [field, message]))
      : fields || {};
  }
}

let csrfRequest;
let unauthorizedHandler;

export function onUnauthorized(handler) {
  unauthorizedHandler = handler;
}

export function clearCsrf() {
  csrfRequest = undefined;
}

async function readResponse(response) {
  if (response.status === 204 || response.status === 205) return null;
  if (response.headers.get('content-type')?.includes('json')) return response.json();
  return null;
}

async function send(path, options, handleUnauthorized = true) {
  let response;
  try {
    response = await fetch(path, { credentials: 'same-origin', redirect: 'error', ...options });
  } catch (error) {
    if (error.name === 'AbortError') throw error;
    throw new ApiError(0, { detail: 'Não foi possível conectar. Confira a internet e tente novamente.' });
  }
  if (response.status === 401 && handleUnauthorized) {
    clearCsrf();
    unauthorizedHandler?.();
  }
  const value = await readResponse(response);
  if (!response.ok) throw new ApiError(response.status, value);
  return value;
}

function getCsrf() {
  csrfRequest ??= send('/api/v1/auth/csrf', {
    headers: { Accept: 'application/json' },
    cache: 'no-store',
  }).catch((error) => {
    clearCsrf();
    throw error;
  });
  return csrfRequest;
}

export function read(path, { signal, handleUnauthorized = true } = {}) {
  return send('/api/v1' + path, {
    signal,
    headers: { Accept: 'application/json' },
  }, handleUnauthorized);
}

export async function write(path, method, body, { key, absolute = false, signal } = {}) {
  const { token, headerName } = await getCsrf();
  const headers = { Accept: 'application/json', [headerName]: token };
  const isForm = body instanceof FormData;
  if (body !== undefined && !isForm) headers['Content-Type'] = 'application/json';
  if (key) headers['Idempotency-Key'] = key;
  return send(absolute ? path : '/api/v1' + path, {
    method,
    signal,
    headers,
    body: isForm || body === undefined ? body : JSON.stringify(body),
  });
}

export async function logout() {
  // A successful Spring logout may redirect back to the React login page.
  const { token, headerName } = await getCsrf();
  const response = await fetch('/logout', {
    method: 'POST',
    credentials: 'same-origin',
    redirect: 'follow',
    headers: { [headerName]: token },
  });
  if (!response.ok) throw new ApiError(response.status, await readResponse(response));
  clearCsrf();
}

export function pageParams(page, busca) {
  const params = new URLSearchParams({ page });
  if (busca?.trim()) params.set('busca', busca.trim());
  return '?' + params;
}

export function normalizePage(value) {
  if (Array.isArray(value)) return { rows: value, page: 0, totalPages: 1 };
  return {
    rows: value.content ?? value.itens,
    page: value.number ?? value.pagina ?? 0,
    totalPages: value.totalPages ?? value.totalPaginas ?? 1,
  };
}

export async function allPages(path, signal) {
  const rows = [];
  let page = 0;
  let totalPages;
  do {
    const result = normalizePage(await read(path + pageParams(page), { signal }));
    rows.push(...result.rows);
    totalPages = result.totalPages;
    page += 1;
  } while (page < totalPages);
  return rows;
}

import { useEffect, useRef, useState } from 'react';

export function useLoad(loader, inputs) {
  const [revision, setRevision] = useState(0);
  const [state, setState] = useState({ loading: true });
  useEffect(() => {
    const controller = new AbortController();
    setState({ loading: true });
    loader(controller.signal).then((data) => {
      if (!controller.signal.aborted) setState({ data });
    }).catch((error) => {
      if (!controller.signal.aborted) setState({ error });
    });
    return () => controller.abort();
  }, [...inputs, revision]);
  return { ...state, reload: () => setRevision((value) => value + 1) };
}

export function useAction() {
  const [pending, setPending] = useState(false);
  const [error, setError] = useState(null);
  const request = useRef(null);
  const locked = useRef(false);
  async function run(operation, payload) {
    if (locked.current) return;
    locked.current = true;
    setPending(true);
    setError(null);
    const signature = JSON.stringify(payload);
    try {
      if (!request.current || request.current.signature !== signature) request.current = { signature, key: crypto.randomUUID() };
      const result = await operation(request.current.key);
      request.current = null;
      return result;
    } catch (failure) {
      setError(failure);
      return undefined;
    } finally {
      locked.current = false;
      setPending(false);
    }
  }
  return { pending, error, run, clearError: () => setError(null) };
}

import { defineConfig, loadEnv } from 'vite';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const target = env.DOCEUP_BACKEND_ORIGIN || 'http://localhost:8080';
  return {
    esbuild: { jsx: 'automatic' },
    server: {
      proxy: Object.fromEntries(
        ['/api', '/oauth2', '/login/oauth2', '/logout'].map((path) => [path, { target }]),
      ),
    },
  };
});

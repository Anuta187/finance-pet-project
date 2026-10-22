import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Все запросы /api проксируются на бэкенд Spring Boot (по умолчанию :3000).
// API_TARGET позволяет направить прокси на другой адрес, WEB_PORT — сменить порт фронтенда.
const apiTarget = process.env.API_TARGET || 'http://localhost:3000';

export default defineConfig({
  plugins: [react()],
  server: {
    port: Number(process.env.WEB_PORT || 5173),
    strictPort: true,
    proxy: {
      '/api': { target: apiTarget, changeOrigin: true },
    },
  },
});

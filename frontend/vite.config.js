import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// In development the screen runs on :5173 and forwards /api calls to the Spring Boot backend on :8080.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: { '/api': 'http://localhost:8080' },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/setupTests.js',
  },
})

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// O navegador só fala com o Vite (mesma origem), e o Vite repassa /api para o backend.
// Assim o backend não precisa liberar CORS para ninguém.
const proxy = {
  '/api': process.env.BACKEND_URL ?? 'http://localhost:8080',
}

export default defineConfig({
  plugins: [vue()],
  server: { proxy },
  preview: { proxy },
})

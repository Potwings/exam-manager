import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [vue(), tailwindcss()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    server: {
      host: '0.0.0.0',
      port: 5173,
      allowedHosts: env.ALLOWED_HOSTS ? env.ALLOWED_HOSTS.split(',').filter(Boolean) : [],
      proxy: {
        '/api': {
          target: env.API_TARGET || 'http://localhost:8080',
          changeOrigin: true
        }
      }
    }
  }
})

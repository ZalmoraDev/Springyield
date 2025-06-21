import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  server: {
    allowedHosts: ['springyield.stefvisser.com', 'localhost', 'springyield.local'],
    host: '0.0.0.0',        // Listen on all network interfaces
    strictPort: false,      // Allow fallback to another port if needed
    cors: true,             // Enable CORS for all origins
    hmr: {
      clientPort: 443,      // Connect to WebSocket through HTTPS port if using reverse proxy
      host: 'springyield.stefvisser.com'  // Specify your domain for HMR connections
    }
  }
})

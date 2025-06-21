import { fileURLToPath, URL } from 'node:url'
import tailwindcss from '@tailwindcss/vite'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
    tailwindcss()
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  server: {
    allowedHosts: ['springyield.stefvisser.com', 'localhost'],
    host: '0.0.0.0',        // Listen on all network interfaces
    port: 5173,            // Specify the port for the dev server
    strictPort: true,      // Do not fall back to another port if 20300 is in use
    cors: true,             // Enable CORS for all origins
    // HMR settings removed for local development, Vite will handle it.
    hmr: {
      clientPort: 20300
    },
    watch: {
      usePolling: true
    }
  }
})

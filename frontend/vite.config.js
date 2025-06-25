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
    host: '0.0.0.0',
    port: 5173,
    strictPort: true,
    hmr: {
      clientPort: 5173
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      }
    }
  }

  // server: {
  //   allowedHosts: ['localhost', 'stefvisser.com', 'springyield.stefvisser.com'], // Allow these hosts
  //   host: '0.0.0.0',        // Listen on all network interfaces
  //   port: 5173,            // Specify the port for the dev server
  //   strictPort: true,      // Do not fall back to another port if 20300 is in use
  //   cors: true,             // Enable CORS for all origins
  //   // HMR settings removed for local development, Vite will handle it.
  //   hmr: {
  //     clientPort: 20300
  //   },
  //   watch: {
  //     usePolling: true
  //   }
  // }
})

import './styles.css'
import { createApp } from 'vue'
import { router } from './router/router.js'

import App from './App.vue' // Root app containing <RouterView />

createApp(App).use(router).mount('#app')

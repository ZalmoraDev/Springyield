<script setup>
import {ref} from 'vue'
import {useRouter} from 'vue-router'
import axios from 'axios'
import {API_BASE_URL} from '@/config.js'; // <-- Import API_BASE_URL

const router = useRouter()

const email = ref('')
const password = ref('')
const errorMessage = ref('')

const handleLogin = async () => {
  errorMessage.value = ''
  try {
    const response = await axios.post(`${API_BASE_URL}/auth/login`, {
      email: email.value,
      password: password.value
    })

    const {token, user} = response.data
    localStorage.setItem('token', token)
    localStorage.setItem('user', JSON.stringify(user))
    await router.push('/')

  } catch (error) {
    errorMessage.value = error.response?.data || 'An error occurred during login';
  }
}
</script>


<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-200">
    <div class="w-full max-w-md bg-white rounded-2xl shadow-xl p-8">
      <img src="/logo/logo-bt.svg" alt="Logo" class="w-36 h-36 mx-auto mb-2">
      <h1 class="text-2xl font-bold text-center mb-6">Login</h1>
      <div v-if="errorMessage" class="error-message">
        ⛔{{ errorMessage }}
      </div>
      <form @submit.prevent="handleLogin">

        <div class="mb-4">
          <label class="block text-sm font-medium text-gray-700 mb-1">Email</label>
          <input
              v-model="email"
              type="email"
              class="w-full px-4 py-2 border rounded-xl focus:outline-none focus:ring-2 focus:ring-sy-500"
              required
          />
        </div>
        <div class="mb-6">
          <label class="block text-sm font-medium text-gray-700 mb-1">Password</label>
          <input
              v-model="password"
              type="password"
              class="w-full px-4 py-2 border rounded-xl focus:outline-none focus:ring-2 focus:ring-sy-500"
              required
          />
        </div>

        <button
            type="submit"
            class="w-full bg-sy-600 text-white py-2 rounded-xl hover:bg-sy-700 transition cursor-pointer"
        >
          Log In
        </button>
      </form>
      <div class="flex items-center justify-between">
        <hr class="my-6 border-gray-300"/>
        <span class="text-gray-600">OR</span>
        <hr class="my-6 border-gray-300"/>
      </div>
      <router-link
          to="/signup"
          class="block w-full bg-gray-900 text-white py-2 rounded-xl text-center hover:bg-gray-700 transition cursor-pointer"
      >
        Sign-up
      </router-link>

    </div>
  </div>
</template>

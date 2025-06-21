<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { API_BASE_URL } from '@/config.js'; // Import API_BASE_URL

const router = useRouter()

const firstName = ref('')
const lastName = ref('')
const bsnNumber = ref('')
const email = ref('')
const phoneNumber = ref('')
const password = ref('')
const confirmPassword = ref('')

const handleSignup = async () => {
  if (password.value !== confirmPassword.value) {
    alert("Wachtwoorden komen niet overeen")
    return
  }

  try {
    const response = await axios.post(`${API_BASE_URL}/auth/signup`, { // Use API_BASE_URL
      firstName: firstName.value,
      lastName: lastName.value,
      bsnNumber: Number(bsnNumber.value),
      email: email.value,
      phoneNumber: phoneNumber.value,
      password: password.value // Plaintext, to be salted and hashed in the backend
    })

    const { token, user } = response.data
    localStorage.setItem('token', token)
    localStorage.setItem('user', JSON.stringify(user))

    await router.push('/')

  } catch (error) {
    alert(error.response?.data?.message || 'Registratie mislukt')
  }
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-200">
    <div class="w-full max-w-md bg-white rounded-2xl shadow-xl p-8">
      <router-link
          to="/login"
          class="inline-flex items-center text-gray-700 p-1 rounded-xl mb-2 hover:bg-gray-200 transition cursor-pointer"
      >
        <img src="/arrow_back_24dp_000.svg" alt="Arrow left" class="w-6 h-6 mr-2">
        <span>Terug naar inloggen</span>
      </router-link>
      <img src="/logo/logo-bt.svg" alt="Logo" class="w-24 h-24 mx-auto mb-2">
      <h1 class="text-2xl font-bold text-center mb-6">Registreren</h1>
      <form class="flex flex-col gap-4" @submit.prevent="handleSignup">

        <div class="flex flex-col gap-2">
          <div class="flex flex-row gap-2">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Voornaam *</label>
              <input
                  v-model="firstName"
                  type="text"
                  class="w-full px-4 py-2 border rounded-xl focus:outline-none focus:ring-2 focus:ring-sy-500"
                  required
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Achternaam *</label>
              <input
                  v-model="lastName"
                  type="text"
                  class="w-full px-4 py-2 border rounded-xl focus:outline-none focus:ring-2 focus:ring-sy-500"
                  required
              />
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">BSN / Persoonsnummer *</label>
            <input
                v-model="bsnNumber"
                type="number"
                class="w-full px-4 py-2 border rounded-xl focus:outline-none focus:ring-2 focus:ring-sy-500"
                required
            />
          </div>
        </div>

        <div class="gap-2 flex flex-col">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Email *</label>
            <input
                v-model="email"
                type="email"
                class="w-full px-4 py-2 border rounded-xl focus:outline-none focus:ring-2 focus:ring-sy-500"
                required
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Telefoonnummer *</label>
            <input
                v-model="phoneNumber"
                type="tel"
                class="w-full px-4 py-2 border rounded-xl focus:outline-none focus:ring-2 focus:ring-sy-500"
                required
            />
          </div>
        </div>

        <div class="gap-2 flex flex-col">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Wachtwoord *</label>
            <input
                v-model="password"
                type="password"
                class="w-full px-4 py-2 border rounded-xl focus:outline-none focus:ring-2 focus:ring-sy-500"
                required
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Herhaal Wachtwoord *</label>
            <input
                v-model="confirmPassword"
                type="password"
                class="w-full px-4 py-2 border rounded-xl focus:outline-none focus:ring-2 focus:ring-sy-500"
                required
            />
          </div>
        </div>

        <button
            type="submit"
            class="w-full bg-sy-600 text-white mb-2 py-2 rounded-xl hover:bg-sy-700 transition cursor-pointer"
        >
          Gebruiker Aanmaken
        </button>
        <span class="text-gray-500">* vereiste velden</span>
      </form>
    </div>
  </div>
</template>

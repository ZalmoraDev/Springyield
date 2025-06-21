<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import Sidebar from '@/components/adminComps/Sidebar.vue'
import Toast from '@/components/adminComps/Toast.vue'
import { API_BASE_URL } from '@/config.js'; // Import API_BASE_URL

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const error = ref(null)
const t = ref(null)
const fromAccount = ref(null)
const toAccount = ref(null)

const storedUser = computed(() => JSON.parse(localStorage.getItem('user')))
const role = computed(() => storedUser.value?.role || localStorage.getItem('role'))
const authToken = computed(() => localStorage.getItem('token'))

const showToast = ref(false)
const toastMessage = ref('')

const fetchTransactionDetails = async () => {
  try {
    loading.value = true
    error.value = null
    const transactionId = route.params.transactionId

    if (!authToken.value) {
      error.value = "Authentication token not found. Please log in.";
      loading.value = false;
      return;
    }

    // Using the proper endpoint to fetch transaction by ID
    const response = await axios.get(`${API_BASE_URL}/transactions/id/${transactionId}`, {
      headers: {
        'Authorization': `Bearer ${authToken.value}`
      }
    })

    if (response.data) {
      t.value = response.data
      // You could fetch additional account details here if needed
    } else {
      error.value = 'Failed to fetch transaction details or no data returned.'
    }

  } catch (err) {
    if (err.response) {
      error.value = `Error ${err.response.status}: ${err.response.data.message || 'Could not fetch transaction details.'}`
      if (err.response.status === 401) {
        error.value = 'Unauthorized: Your session may have expired. Please log in again.'
      } else if (err.response.status === 404) {
        error.value = 'Transaction not found.'
      }
    } else if (err.request) {
      error.value = 'Network error: Could not connect to the server.'
    } else {
      error.value = 'An unexpected error occurred.'
    }
  } finally {
    loading.value = false
  }
}

const formatIban = (iban) => {
  if (!iban) return 'N/A'
  const clean = String(iban).replace(/\s/g, '')
  return clean.match(/.{1,4}/g)?.join(' ') || clean
}

const formatCurrency = (amount) => {
  if (typeof amount !== 'number') return 'N/A';
  return `${amount < 0 ? '-' : ''}\u20AC${Math.abs(amount).toLocaleString('nl-NL', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

const formatDate = (dateString) => {
  if (!dateString) return 'N/A';
  return new Date(dateString).toLocaleString('nl-NL', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
}

const goBack = () => {
  router.push({name: 'Transactions'}) // Navigate back to the transactions overview page
}

onMounted(() => {
  fetchTransactionDetails()
})
</script>


<template>
  <div class="flex min-h-screen bg-gray-100">
    <Sidebar :router="router" :user="storedUser" :role="role"/>
    <Toast :show="showToast" :message="toastMessage" @close="showToast = false"/>

    <div class="flex-1 p-8">
      <div class="mb-6">
        <button @click="goBack"
                class="px-4 py-2 flex rounded-2xl bg-white hover:bg-gray-100 border-gray-300 border cursor-pointer">
          <img class="pointer-events-none flex items-center text-gray-700"
               src="/arrow_back_24dp_000.svg"
               alt="Back-arrow Icon">Back to Transactions
        </button>
      </div>

      <div v-if="error" class="mb-4 p-4 bg-red-100 text-red-700 border border-red-200 rounded-lg shadow">
        <h3 class="font-bold text-lg mb-2">Error Fetching Transaction Details</h3>
        <p>{{ error }}</p>
      </div>

      <div v-if="loading" class="text-center py-10">
        <div class="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-blue-500 mx-auto"></div>
        <p class="mt-4 text-gray-600 text-lg">Loading Transaction Details...</p>
      </div>

      <div v-else-if="t" class="bg-white rounded-xl shadow-xl p-6 md:p-8">
        <div class="mb-8">
          <h1 class="text-3xl font-bold text-gray-800">Transaction Details</h1>
          <p class="text-lg text-gray-500 mt-1">Transaction ID: <span class="font-mono">{{ t.transactionId }}</span></p>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-6 mb-8">
          <div class="bg-gray-50 p-4 rounded-lg ring-1 ring-gray-200">
            <p class="text-sm text-gray-500">From Account</p>
            <p class="font-medium text-lg text-gray-700 truncate">{{ formatIban(t.fromAccount) }}</p>
          </div>
          <div class="bg-gray-50 p-4 rounded-lg ring-1 ring-gray-200">
            <p class="text-sm text-gray-500">To Account</p>
            <p class="font-medium text-lg text-gray-700 truncate">{{ formatIban(t.toAccount) }}</p>
          </div>
          <div class="bg-gray-50 p-4 rounded-lg ring-1 ring-gray-200">
            <p class="text-sm text-gray-500">Amount</p>
            <p class="font-medium text-xl" :class="{'text-green-600': t.transferAmount >= 0, 'text-red-600': t.transferAmount < 0}">
              {{ formatCurrency(t.transferAmount) }}
            </p>
          </div>
          <div class="bg-gray-50 p-4 rounded-lg ring-1 ring-gray-200">
            <p class="text-sm text-gray-500">Transaction Type</p>
            <p class="font-medium text-lg">
              <span
                :class="{
                  'bg-amber-300 text-amber-800': t.transactionType === 'TRANSFER',
                  'bg-red-300 text-red-800': t.transactionType === 'DEPOSIT',
                  'bg-green-300 text-green-800': t.transactionType === 'WITHDRAW'
                }"
                class="px-2 py-1 inline-block text-sm leading-5 font-semibold rounded-full"
              >
                {{ t.transactionType }}
              </span>
            </p>
          </div>
          <div class="bg-gray-50 p-4 rounded-lg ring-1 ring-gray-200">
            <p class="text-sm text-gray-500">Reference</p>
            <p class="font-medium text-lg text-gray-700 truncate">{{ t.reference || 'N/A' }}</p>
          </div>
          <div class="bg-gray-50 p-4 rounded-lg ring-1 ring-gray-200">
            <p class="text-sm text-gray-500">Date & Time</p>
            <p class="font-medium text-lg text-gray-700 truncate">{{ formatDate(t.timestamp) }}</p>
          </div>
        </div>

      </div>
      <div v-else-if="!loading && !error" class="text-center py-10">
        <p class="text-gray-500 text-xl">No transaction details could be loaded.</p>
      </div>
    </div>
  </div>
</template>

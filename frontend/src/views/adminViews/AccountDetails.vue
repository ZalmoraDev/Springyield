<script setup>
import {ref, onMounted, computed} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import axios from 'axios'
import Sidebar from '@/components/adminComps/Sidebar.vue'
import Toast from '@/components/adminComps/Toast.vue'
import {API_BASE_URL} from '@/config.js'
import {formatCurrency} from '@/utils/formatters.js'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const error = ref(null)
const a = ref(null)
const owner = ref(null)
const editableAccount = ref(null) // For editing account limits

const storedUser = computed(() => JSON.parse(localStorage.getItem('user')))
const role = computed(() => storedUser.value?.role || localStorage.getItem('role'))
const authToken = computed(() => localStorage.getItem('token'))

const showToast = ref(false)
const toastMessage = ref('')

// Edit mode state (replacing editingLimits)
const isEditing = ref(false)

const fetchAccountDetails = async () => {
  try {
    loading.value = true
    error.value = null
    const accountId = route.params.accountId

    const response = await axios.get(`${API_BASE_URL}/account/search`, {
      params: {
        limit: 1000,
        offset: 0
      },
      headers: {
        'Authorization': `Bearer ${authToken.value}`
      }
    })

    if (response.data?.data) {
      const foundAccount = response.data.data.find(acc => String(acc.accountId) === String(accountId))
      if (foundAccount) {
        a.value = foundAccount
        if (foundAccount.user) {
          owner.value = foundAccount.user
        }
        // Initialize editableAccount when account data is fetched
        editableAccount.value = JSON.parse(JSON.stringify(foundAccount))
      } else {
        error.value = 'Account not found.'
      }
    } else {
      error.value = 'Failed to fetch account details or no data returned.'
    }
  } catch (err) {
    handleError(err)
  } finally {
    loading.value = false
  }
}

const handleError = (err) => {
  if (err.response) {
    error.value = `Error ${err.response.status}: ${err.response.data.message || 'Could not fetch account details.'}`
    if (err.response.status === 401) {
      error.value = 'Unauthorized: Your session may have expired. Please log in again.'
    }
  } else if (err.request) {
    error.value = 'Network error: Could not connect to the server.'
  } else {
    error.value = 'An unexpected error occurred.'
  }
}

// Toggle edit mode (replaces startEditingLimits)
const toggleEditMode = () => {
  if (!isEditing.value) {
    // If starting edit, make a deep copy of the current account data
    editableAccount.value = JSON.parse(JSON.stringify(a.value))
  }
  isEditing.value = !isEditing.value
}

// Discard changes (replaces cancelEditLimits)
const discardChanges = () => {
  // Reset editableAccount to original account data and exit edit mode
  editableAccount.value = JSON.parse(JSON.stringify(a.value))
  isEditing.value = false
}

// Save account changes (replaces saveLimits)
const saveChanges = async () => {
  try {
    // Basic validation
    if (editableAccount.value.dailyLimit < 0 || editableAccount.value.absoluteLimit < 0) {
      error.value = 'Limits cannot be negative'
      return
    }

    loading.value = true
    const response = await axios.put(
        `${API_BASE_URL}/account/${a.value.accountId}/limits`,
        {
          dailyLimit: editableAccount.value.dailyLimit,
          absoluteLimit: editableAccount.value.absoluteLimit
        },
        {
          headers: {
            'Authorization': `Bearer ${authToken.value}`
          }
        }
    )

    a.value.dailyLimit = response.data.dailyLimit
    a.value.absoluteLimit = response.data.absoluteLimit
    isEditing.value = false
    toastMessage.value = 'Account limits updated successfully'
    showToast.value = true
    setTimeout(() => showToast.value = false, 3000)

    // Refresh account details
    await fetchAccountDetails()
  } catch (err) {
    error.value = err.response?.data?.message || 'Failed to update limits'
  } finally {
    loading.value = false
  }
}

const formatIban = (iban) => {
  if (!iban) return 'N/A'
  const clean = String(iban).replace(/\s/g, '')
  return clean.match(/.{1,4}/g)?.join(' ') || clean
}

const goBack = () => {
  router.push({name: 'Accounts'})
}

onMounted(() => {
  fetchAccountDetails()
})
</script>


<template>
  <div class="flex min-h-screen bg-gray-100">
    <Sidebar :router="router" :user="storedUser" :role="role"/>
    <Toast :show="showToast" :message="toastMessage" @close="showToast = false"/>

    <div class="flex-1 p-8">
      <div class="mb-6">
        <div class="flex gap-4 items-center mb-6">
          <button @click="goBack"
                  class="px-4 py-2 flex rounded-2xl bg-white hover:bg-gray-100 border-gray-300 border cursor-pointer">
            <img class="pointer-events-none flex items-center text-gray-700"
                 src="/arrow_back_24dp_000.svg"
                 alt="Back-arrow Icon">
            Back to Accounts
          </button>
          <div class="flex gap-2">
            <!-- Edit Account button - visible when not in edit mode -->
            <button v-if="!isEditing && a"
                    @click="toggleEditMode"
                    class="flex gap-2 items-center px-4 py-2 rounded-2xl text-white bg-blue-500 hover:bg-blue-600 font-bold border-gray-300 border cursor-pointer"
            >
              <img
                  src="/edit_24dp_FFF.svg"
                  alt="Edit Account Icon"
                  class="w-5 h-5 flex-shrink-0">
              Edit Account
            </button>

            <!-- Save button - visible when in edit mode -->
            <button v-if="isEditing"
                    @click="saveChanges"
                    class="flex gap-2 items-center px-4 py-2 rounded-2xl text-white bg-green-500 hover:bg-green-600 font-bold border-gray-300 border cursor-pointer"
            >
              <img
                  src="/check_24dp_FFF.svg"
                  alt="Save Changes Icon"
                  class="w-5 h-5 flex-shrink-0">
              Save Changes
            </button>

            <!-- Discard button - visible when in edit mode -->
            <button v-if="isEditing"
                    @click="discardChanges"
                    class="flex gap-2 items-center px-4 py-2 rounded-2xl text-gray-700 bg-gray-200 hover:bg-gray-300 font-bold border-gray-300 border cursor-pointer"
            >
              <img
                  src="/cancel_24dp_000.svg"
                  alt="Discard Changes Icon"
                  class="w-5 h-5 flex-shrink-0">
              Discard Changes
            </button>
          </div>
        </div>
      </div>

      <div v-if="error" class="mb-4 p-4 bg-red-100 text-red-700 border border-red-200 rounded-lg shadow">
        <h3 class="font-bold text-lg mb-2">Error</h3>
        <p>{{ error }}</p>
      </div>

      <div v-if="loading && !isEditing" class="text-center py-10">
        <div class="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-blue-500 mx-auto"></div>
        <p class="mt-4 text-gray-600 text-lg">Loading Account Details...</p>
      </div>

      <div v-else-if="a && editableAccount" class="bg-white rounded-xl shadow-xl p-6 md:p-8">
        <div class="mb-8">
          <h1 class="text-3xl font-bold text-gray-800">Account Details</h1>
          <p class="text-lg text-gray-500 mt-1">IBAN: <span class="font-mono">{{ formatIban(a.iban) }}</span></p>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-6 mb-8">
          <div class="bg-gray-50 p-4 rounded-lg ring-1 ring-gray-200">
            <p class="text-sm text-gray-500">Account ID</p>
            <p class="font-medium text-lg text-gray-700 truncate">{{ a.accountId }}</p>
          </div>
          <div class="bg-gray-50 p-4 rounded-lg ring-1 ring-gray-200">
            <p class="text-sm text-gray-500">Current Balance</p>
            <p class="font-medium text-xl truncate" :class="{'text-green-500': a.balance >= 0, 'text-red-500': a.balance < 0}">
              {{ formatCurrency(a.balance) }}
            </p>
          </div>
          <div class="bg-gray-50 p-4 rounded-lg ring-1 ring-gray-200">
            <p class="text-sm text-gray-500">Account Type</p>
            <span
                :class="{
                  'bg-sy-300 text-sy-800': a.accountType === 'PAYMENT',
                  'bg-amber-300 text-amber-800': a.accountType === 'SAVINGS',
                  'bg-black text-white': a.accountType === 'ATM'
                }"
                class="px-2 py-1 inline-block text-sm leading-5 font-semibold rounded-full"
            >
                {{ a.accountType }}
              </span>
          </div>
          <div class="bg-gray-50 p-4 rounded-lg ring-1 ring-gray-200">
            <p class="text-sm text-gray-500">Status</p>
            <span
                :class="{
                  'px-2 py-1 inline-block text-sm leading-5 font-semibold rounded-full': true,
                  'bg-green-300 text-green-800': a.status === 'ACTIVE',
                  'bg-neutral-800 text-neutral-100': a.status === 'DEACTIVATED'
                }"
            >
              {{ a.status }}
            </span>
          </div>
        </div>

        <!-- Section for account limits -->
        <div class="mb-8 pt-8 border-t border-gray-200">
          <h3 class="text-xl font-semibold text-gray-700 mb-4">Account Limits</h3>

          <div v-if="!isEditing" class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="bg-gray-50 p-4 rounded-lg ring-1 ring-gray-200">
              <p class="text-sm text-gray-500">Daily Transfer Limit</p>
              <p class="font-medium text-lg text-gray-700">€{{ a.dailyLimit }}</p>
            </div>
            <div class="bg-gray-50 p-4 rounded-lg ring-1 ring-gray-200">
              <p class="text-sm text-gray-500">Absolute Transfer Limit</p>
              <p class="font-medium text-lg text-gray-700">€{{ a.absoluteLimit }}</p>
            </div>
          </div>

          <div v-if="isEditing" class="space-y-4">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">Daily Transfer Limit (€)</label>
                <div class="relative rounded-md shadow-sm">
                  <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <span class="text-gray-500 sm:text-sm">€</span>
                  </div>
                  <input
                      type="number"
                      v-model.number="editableAccount.dailyLimit"
                      class="block w-full pl-8 pr-3 py-3 border-2 border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white hover:border-gray-400 transition-colors"
                      min="0"
                      step="0.01"
                      placeholder="Enter daily limit"
                  />
                </div>
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">Absolute Transfer Limit (€)</label>
                <div class="relative rounded-md shadow-sm">
                  <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <span class="text-gray-500 sm:text-sm">€</span>
                  </div>
                  <input
                      type="number"
                      v-model.number="editableAccount.absoluteLimit"
                      class="block w-full pl-8 pr-3 py-3 border-2 border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white hover:border-gray-400 transition-colors"
                      min="0"
                      step="0.01"
                      placeholder="Enter absolute limit"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="owner" class="mt-10 pt-8 border-t border-gray-200">
          <h2 class="text-2xl font-semibold mb-6 text-gray-700">Owner Information</h2>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-6">
            <div class="bg-gray-50 p-4 rounded-lg ring-1 ring-gray-200">
              <p class="text-sm text-gray-500">Owner ID</p>
              <p class="font-medium text-lg text-gray-700 truncate">{{ owner.userId }}</p>
            </div>
            <div class="bg-gray-50 p-4 rounded-lg ring-1 ring-gray-200">
              <p class="text-sm text-gray-500">Owner Name</p>
              <p class="font-medium text-lg text-gray-700 truncate">{{ owner.firstName }} {{ owner.lastName }}</p>
            </div>
            <div v-if="owner.email" class="bg-gray-50 p-4 rounded-lg ring-1 ring-gray-200 md:col-span-2">
              <p class="text-sm text-gray-500">Owner Email</p>
              <p class="font-medium text-lg text-gray-700 truncate">{{ owner.email }}</p>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="!loading && !error" class="text-center py-10">
        <p class="text-gray-500 text-xl">No account details could be loaded.</p>
      </div>
    </div>
  </div>
</template>

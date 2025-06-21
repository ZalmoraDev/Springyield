<template>
  <div v-if="show"
       class="fixed inset-0 bg-green-500/20 bg-opacity-50 backdrop-blur-sm overflow-y-auto h-full w-full flex items-center justify-center z-50">
    <div class="p-8 border w-96 shadow-lg rounded-md bg-white">
      <div class="text-center">
        <h3 class="text-2xl font-bold text-gray-900">Confirm Approval</h3>
        <div class="mt-2 px-7 py-3">
          <p class="text-lg text-gray-500 overflow-hidden">
            Set limits for {{ user?.firstName }} {{ user?.lastName }}'s accounts
          </p>
          <!-- Add input fields for limits -->
          <div class="mt-4">
            <div class="mb-3">
              <label class="block text-sm font-medium text-gray-700">Daily Transfer Limit (€)</label>
              <input
                  type="number"
                  v-model="dailyLimit"
                  class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-green-500 focus:ring-green-500"
                  min="0"
                  step="0.01"
              />
            </div>
            <div class="mb-3">
              <label class="block text-sm font-medium text-gray-700">Absolute Transfer Limit (€)</label>
              <input
                  type="number"
                  v-model="absoluteLimit"
                  class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-green-500 focus:ring-green-500"
                  min="0"
                  step="0.01"
              />
            </div>
          </div>
        </div>
        <div class="flex justify-center mt-4 space-x-4">
          <button
              @click="$emit('close')"
              class="px-4 py-2 bg-gray-300 text-gray-800 rounded hover:bg-gray-400 cursor-pointer"
          >
            Cancel
          </button>
          <button
              @click="handleConfirm"
              class="px-4 py-2 bg-green-500 text-white rounded hover:bg-green-600 cursor-pointer"
          >
            Confirm
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import axios from 'axios'
import {API_BASE_URL} from '@/config.js'
import {ref} from 'vue'

const props = defineProps({
  show: Boolean,
  user: Object,
  token: String
})

const emit = defineEmits(['close', 'approve-success', 'approve-error'])
const dailyLimit = ref(1000) // Default value
const absoluteLimit = ref(500) // Default value

const handleConfirm = async () => {
  if (!props.user?.userId || !props.token) {
    emit('approve-error', 'Missing required data')
    return
  }

  try {
    await axios.put(
        `${API_BASE_URL}/user/${props.user.userId}/approve`,
        {
          dailyLimit: dailyLimit.value,
          absoluteLimit: absoluteLimit.value
        },
        {
          headers: {
            Authorization: `Bearer ${props.token}`
          }
        }
    )
    emit('approve-success', props.user)
  } catch (err) {
    emit('approve-error', err.response?.data?.message || 'Failed to approve user.')
  }
}
</script>
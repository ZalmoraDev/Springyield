<template>
  <div v-if="show" class="fixed inset-0 bg-red-500/20 bg-opacity-50 backdrop-blur-sm overflow-y-auto h-full w-full flex items-center justify-center z-50">
    <div class="p-8 border w-96 shadow-lg rounded-md bg-white" :class="{ 'animate-spin-fast': spinning }">
      <div class="text-center">
        <h3 class="text-2xl font-bold text-gray-900">Confirm Deletion</h3>
        <div class="mt-2 px-7 py-3">
          <p class="text-lg text-gray-500 overflow-hidden">
            Are you sure you want to <strong class="text-black">delete user {{ user?.firstName }} {{ user?.lastName }}</strong>?
          </p>
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
              :disabled="countdown > 0"
              :class="{
                'px-4 py-2 rounded': true,
                'bg-gray-400 text-gray-600 cursor-not-allowed': countdown > 0,
                'bg-red-500 text-white hover:bg-red-600 cursor-pointer': countdown === 0
              }"
          >
            {{ countdown > 0 ? `Delete (${countdown})` : 'Delete' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import axios from 'axios' // Import axios
import { API_BASE_URL } from '@/config.js'; // Import API_BASE_URL
import { ref, onMounted, onUnmounted, watch } from 'vue' // Add ref, onMounted, onUnmounted, and watch

const props = defineProps({
  show: Boolean,
  user: Object,
  spinning: {
    type: Boolean,
    default: false
  },
  token: String // Add token prop
})

const emit = defineEmits(['close', 'delete-success', 'delete-error']) // Updated emits

// Add countdown functionality
const countdown = ref(3)
let countdownTimer = null

const startCountdown = () => {
  countdown.value = 3
  clearInterval(countdownTimer)
  countdownTimer = setInterval(() => {
    if (countdown.value > 0) {
      countdown.value--
    } else {
      clearInterval(countdownTimer)
    }
  }, 1000)
}

// Watch for modal visibility changes
watch(() => props.show, (newValue) => {
  if (newValue) {
    startCountdown()
  } else {
    clearInterval(countdownTimer)
  }
})

// Start countdown when modal is shown
onMounted(() => {
  if (props.show) {
    startCountdown()
  }
})

// Clean up timer when component is unmounted
onUnmounted(() => {
  clearInterval(countdownTimer)
})

const handleConfirm = async () => {
  if (countdown.value > 0) return // Don't allow confirmation during countdown

  if (!props.user || !props.user.userId) {
    emit('delete-error', 'User data is missing for deletion.')
    return
  }
  if (!props.token) {
    emit('delete-error', 'Authentication token is missing for deletion.')
    return
  }

  try {
    await axios.delete(`${API_BASE_URL}/user/${props.user.userId}/delete`, {
      headers: {
        Authorization: `Bearer ${props.token}` // Use the token prop for authentication
      }
    })
    emit('delete-success', props.user) // Emit success event with user data
  } catch (err) {
    emit('delete-error', err.response?.data?.message || 'Failed to delete user.') // Emit error event with message
  }
}
</script>

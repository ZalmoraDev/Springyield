<script setup>
import { ref } from 'vue';
import axios from 'axios';

const props = defineProps({
  show: Boolean,
  user: Object,
  token: String
});

const emit = defineEmits(['close', 'delete-success']);

const loading = ref(false);
const error = ref('');

const deleteAccount = async () => {
  loading.value = true;
  error.value = '';

  try {
    await axios.delete(`/api/users/${props.user.id}`, {
      headers: {
        'Authorization': `Bearer ${props.token}`
      }
    });
    emit('delete-success');
  } catch (err) {
    error.value = err.response?.data?.message || 'Failed to delete account';
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <div v-if="show" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
    <div class="bg-white rounded-lg p-6 w-full max-w-md">
      <h2 class="text-xl font-bold mb-4">Delete Account</h2>

      <p class="text-gray-600 mb-6">
        Are you sure you want to delete your account? This action cannot be undone.
      </p>

      <div v-if="error" class="text-red-600 text-sm mb-4">{{ error }}</div>

      <div class="flex justify-end space-x-4">
        <button
            @click="$emit('close')"
            class="px-4 py-2 border rounded-lg hover:bg-gray-100"
        >
          Cancel
        </button>
        <button
            @click="deleteAccount"
            :disabled="loading"
            class="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50"
        >
          {{ loading ? 'Deleting...' : 'Delete Account' }}
        </button>
      </div>
    </div>
  </div>
</template>
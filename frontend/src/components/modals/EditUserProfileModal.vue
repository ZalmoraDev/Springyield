<script setup>
import { ref } from 'vue';
import axios from 'axios';

const props = defineProps({
  show: Boolean,
  user: Object,
  token: String
});

const emit = defineEmits(['close', 'edit-success']);

const formData = ref({
  firstName: props.user?.firstName || '',
  lastName: props.user?.lastName || '',
  email: props.user?.email || '',
  phoneNumber: props.user?.phoneNumber || '',
  password: '',
  confirmPassword: ''
});

const error = ref('');
const loading = ref(false);

const updateUser = async () => {
  if (formData.value.password !== formData.value.confirmPassword) {
    error.value = 'Passwords do not match';
    return;
  }

  loading.value = true;
  error.value = '';

  try {
    const response = await axios.put(`/api/users/${props.user.id}`,
        {
          firstName: formData.value.firstName,
          lastName: formData.value.lastName,
          email: formData.value.email,
          phoneNumber: formData.value.phoneNumber,
          ...(formData.value.password && { password: formData.value.password })
        },
        {
          headers: {
            'Authorization': `Bearer ${props.token}`
          }
        }
    );
    emit('edit-success', response.data);
  } catch (err) {
    error.value = err.response?.data?.message || 'Failed to update user';
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <div v-if="show" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
    <div class="bg-white rounded-lg p-6 w-full max-w-md">
      <h2 class="text-xl font-bold mb-4">Edit Profile</h2>

      <form @submit.prevent="updateUser" class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700">First Name</label>
          <input
              v-model="formData.firstName"
              type="text"
              required
              class="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2"
          />
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700">Last Name</label>
          <input
              v-model="formData.lastName"
              type="text"
              required
              class="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2"
          />
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700">Email</label>
          <input
              v-model="formData.email"
              type="email"
              required
              class="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2"
          />
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700">Phone Number</label>
          <input
              v-model="formData.phoneNumber"
              type="tel"
              required
              class="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2"
          />
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700">New Password (optional)</label>
          <input
              v-model="formData.password"
              type="password"
              class="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2"
          />
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700">Confirm Password</label>
          <input
              v-model="formData.confirmPassword"
              type="password"
              class="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2"
          />
        </div>

        <div v-if="error" class="text-red-600 text-sm">{{ error }}</div>

        <div class="flex justify-end space-x-4 mt-6">
          <button
              type="button"
              @click="$emit('close')"
              class="px-4 py-2 border rounded-lg hover:bg-gray-100"
          >
            Cancel
          </button>
          <button
              type="submit"
              :disabled="loading"
              class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
          >
            {{ loading ? 'Updating...' : 'Save Changes' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
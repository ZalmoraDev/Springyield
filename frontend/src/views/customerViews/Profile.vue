<script setup>
import {ref, onMounted} from 'vue';
import {useRouter} from 'vue-router';
import DeleteUserModal from '@/components/modals/DeleteUserModal.vue';
import axios from 'axios';
import {API_BASE_URL} from '@/config.js';
import AppHeader from "@/components/customerComps/AppHeader.vue";

const router = useRouter();
const user = ref(null);
const showDeleteModal = ref(false);
const authToken = localStorage.getItem('token');
const error = ref('');
const isEditing = ref(false);
const editableUser = ref(null);
const successMessage = ref('');
const password = ref('');
const confirmPassword = ref('');
const showPasswordFields = ref(false);

onMounted(() => {
  const userData = localStorage.getItem('user');
  if (userData) {
    user.value = JSON.parse(userData);
    editableUser.value = JSON.parse(userData); // Initialize editable user
  } else {
    router.push('/login');
  }
});

const toggleEditMode = () => {
  if (!isEditing.value) {
    editableUser.value = JSON.parse(JSON.stringify(user.value));
  }
  isEditing.value = !isEditing.value;
};

const saveChanges = async () => {
  try {
    // Validate required fields
    if (!editableUser.value.firstName?.trim() ||
        !editableUser.value.lastName?.trim() ||
        !editableUser.value.email?.trim() ||
        !editableUser.value.phoneNumber?.trim()) {
      error.value = 'First name, last name, email and phone number are required';
      return;
    }

    // Password validation only if attempting to change it
    if (password.value || confirmPassword.value) {
      if (password.value !== confirmPassword.value) {
        error.value = "Passwords do not match";
        return;
      }
      if (password.value.length < 8) {
        error.value = "Password must be at least 8 characters long";
        return;
      }
    }

    const updateData = {
      firstName: editableUser.value.firstName.trim(),
      lastName: editableUser.value.lastName.trim(),
      email: editableUser.value.email.trim(),
      phoneNumber: editableUser.value.phoneNumber.trim()
    };

    // Only include password if a new one is provided and fields are shown
    if (password.value && confirmPassword.value && password.value === confirmPassword.value)
      updateData.password = password.value;

    const response = await axios.put(
        `${API_BASE_URL}/user/${user.value.userId}/update`,
        updateData,
        {
          headers: {
            'Authorization': `Bearer ${authToken}`,
            'Content-Type': 'application/json'
          }
        }
    );

    if (response.status === 200) {
      const updatedUser = {
        ...user.value,           // Keep existing user data
        ...response.data,        // Apply updates
        userId: user.value.userId // Ensure userId is preserved
      };

      const oldEmail = user.value.email;

      // Update both local state and storage
      user.value = updatedUser;
      localStorage.setItem('user', JSON.stringify(updatedUser));

      // Compare old email with new email from response data
      const emailChanged = oldEmail !== response.data.email;

      // If email or password was updated, force re-login
      if (password.value || emailChanged) {
        console.log('Email changed from', oldEmail, 'to', response.data.email);
        successMessage.value = 'Profile updated successfully. Please log in again with your new credentials.';

        // Force immediate logout
        localStorage.clear(); // Clear all storage
        router.push('/login');
        return;
      }

      isEditing.value = false;
      showPasswordFields.value = false;
      password.value = '';
      confirmPassword.value = '';
      error.value = '';
      successMessage.value = 'Profile updated successfully!';

      setTimeout(() => {
        successMessage.value = '';
      }, 3000);
    }
  } catch (err) {
    error.value = err.response?.data?.message || 'Failed to update user details';
  }
};

const discardChanges = () => {
  editableUser.value = JSON.parse(JSON.stringify(user.value));
  isEditing.value = false;
  showPasswordFields.value = false;
  password.value = '';
  confirmPassword.value = '';
  error.value = '';
};

const handleDeleteSuccess = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
  router.push('/login');
};
</script>

<template>
  <AppHeader :user="user" @logout="logout" @navigateToProfile="navigateToProfile"/>
  <div class="min-h-screen bg-[#191B1D]">
    <div class="container mx-auto px-4 py-8">
      <div v-if="successMessage" class="mb-4 p-4 font-semibold bg-green-100 text-green-700 rounded">
        {{ successMessage }}
      </div>

      <div class="bg-neutral-800 rounded-lg shadow-lg p-6">
        <div class="flex justify-between items-center mb-6">
          <h1 class="text-2xl font-bold text-white">My Profile</h1>
          <div class="space-x-4">
            <button
                v-if="!isEditing"
                @click="toggleEditMode"
                class="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg transition-colors"
            >
              Edit Profile
            </button>
            <template v-if="isEditing">
              <button
                  @click="saveChanges"
                  class="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded-lg transition-colors"
              >
                Save Changes
              </button>
              <button
                  @click="discardChanges"
                  class="bg-gray-600 hover:bg-gray-700 text-white px-4 py-2 rounded-lg transition-colors"
              >
                Discard Changes
              </button>
            </template>
            <button
                v-if="!isEditing"
                @click="showDeleteModal = true"
                class="bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded-lg transition-colors"
            >
              Delete Account
            </button>
          </div>
        </div>

        <div v-if="error" class="mb-4 p-4 font-semibold bg-red-100 text-red-700 rounded">
          {{ error }}
        </div>

        <div v-if="user" class="grid grid-cols-2 gap-4">
          <!-- Left column remains the same -->
          <div class="space-y-4">
            <div>
              <p class="text-sm text-gray-400">First Name</p>
              <div v-if="!isEditing" class="text-white font-medium">{{ user.firstName }}</div>
              <input
                  v-else
                  v-model="editableUser.firstName"
                  type="text"
                  class="mt-1 block w-full px-3 py-2 bg-neutral-700 border border-neutral-600 rounded-md text-white"
              />
            </div>
            <div>
              <p class="text-sm text-gray-400">Last Name</p>
              <div v-if="!isEditing" class="text-white font-medium">{{ user.lastName }}</div>
              <input
                  v-else
                  v-model="editableUser.lastName"
                  type="text"
                  class="mt-1 block w-full px-3 py-2 bg-neutral-700 border border-neutral-600 rounded-md text-white"
              />
            </div>
            <div>
              <p class="text-sm text-gray-400">Email</p>
              <div v-if="!isEditing" class="text-white font-medium">{{ user.email }}</div>
              <input
                  v-else
                  v-model="editableUser.email"
                  type="email"
                  class="mt-1 block w-full px-3 py-2 bg-neutral-700 border border-neutral-600 rounded-md text-white"
              />
            </div>
          </div>

          <!-- Right column with fixed layout -->
          <div class="space-y-4">
            <div>
              <p class="text-sm text-gray-400">Phone Number</p>
              <div v-if="!isEditing" class="text-white font-medium">{{ user.phoneNumber }}</div>
              <input
                  v-else
                  v-model="editableUser.phoneNumber"
                  type="tel"
                  class="mt-1 block w-full px-3 py-2 bg-neutral-700 border border-neutral-600 rounded-md text-white"
              />
            </div>
            <div>
              <p class="text-sm text-gray-400">BSN</p>
              <p class="text-white font-medium mt-[10px]">{{ user.bsnNumber }}</p>
            </div>
            <div>
              <span
                  class="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full mt-[9px]"
                  :class="{
                  'bg-green-300 text-green-800': user.role === 'APPROVED',
                  'bg-red-300 text-red-800': user.role === 'UNAPPROVED',
                  'bg-blue-300 text-blue-800': user.role === 'EMPLOYEE',
                  'bg-black text-white': user.role === 'ADMIN'
                }"
              >
                {{ user.role }}
              </span>
            </div>
          </div>

          <!-- Password fields in new row spanning both columns -->
          <div v-if="isEditing" class="col-span-2 grid grid-cols-2 gap-4">
            <div>
              <p class="text-sm text-gray-400">New Password</p>
              <input
                  v-model="password"
                  type="password"
                  class="mt-1 block w-full px-3 py-2 bg-neutral-700 border border-neutral-600 rounded-md text-white"
                  placeholder="Enter new password"
                  @input="error = ''"
              />
            </div>
            <div>
              <p class="text-sm text-gray-400">Confirm New Password</p>
              <input
                  v-model="confirmPassword"
                  type="password"
                  class="mt-1 block w-full px-3 py-2 bg-neutral-700 border border-neutral-600 rounded-md text-white"
                  placeholder="Confirm new password"
                  @input="error = ''"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <DeleteUserModal
        v-if="showDeleteModal"
        :show="showDeleteModal"
        :user="user"
        :token="authToken"
        @close="showDeleteModal = false"
        @delete-success="handleDeleteSuccess"
    />
  </div>
</template>
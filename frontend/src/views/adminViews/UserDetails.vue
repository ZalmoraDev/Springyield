<script setup>
import {ref, onMounted, computed, watch} from 'vue' // Added watch
import {useRoute, useRouter} from 'vue-router'
import axios from 'axios'
import {API_BASE_URL} from '@/config.js'; // Import API_BASE_URL
import Sidebar from '@/components/adminComps/Sidebar.vue'
import Toast from '@/components/adminComps/Toast.vue'
import DeleteUserModal from '@/components/modals/DeleteUserModal.vue' // Import Delete Modal
import ApproveUserModal from '@/components/modals/ApproveUserModal.vue' // Import Approve Modal

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const error = ref(null)
const user = ref(null)
// const role = ref(null) // Will be replaced by computed property
const storedUser = computed(() => JSON.parse(localStorage.getItem('user'))) // Make storedUser computed
const role = computed(() => storedUser.value?.role || localStorage.getItem('role')) // Role as computed property
const authToken = computed(() => localStorage.getItem('token')) // Auth token

const showToast = ref(false)
const toastMessage = ref('')

// Modal states and controls
const showDeleteModal = ref(false)
const deleteModalSpinning = ref(false) // Delete modal in UserDetails should not spin

const showApproveModal = ref(false)

// Add edit mode state variables
const isEditing = ref(false)
const editableUser = ref(null)

const fetchUserDetails = async () => {
  try {
    loading.value = true
    error.value = null
    const userId = route.params.userId
    const token = localStorage.getItem('token') // authToken.value could also be used here

    const response = await axios.get(`${API_BASE_URL}/user/${userId}`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })

    if (response.data) {
      const foundUser = response.data
      if (foundUser) {
        user.value = foundUser
        // Initialize editableUser when user data is fetched
        editableUser.value = JSON.parse(JSON.stringify(foundUser)) // Deep copy
      } else {
        error.value = 'models.com.stefvisser.springyield.User not found'
      }
    }
  } catch (err) {
    error.value = err.response?.data?.message || 'Failed to fetch user details'
  } finally {
    loading.value = false
  }
}

// Add function to toggle edit mode
const toggleEditMode = () => {
  if (!isEditing.value) {
    // If starting edit, make a deep copy of the user data
    editableUser.value = JSON.parse(JSON.stringify(user.value))
  }
  isEditing.value = !isEditing.value
}

// Add function to save changes with validation
const saveChanges = async () => {
  try {
    // Validate input fields before sending to API
    const validationErrors = [];

    // Check maximum lengths
    if (editableUser.value.firstName.length > 32) {
      validationErrors.push("First name must be 32 characters or less");
    }

    if (editableUser.value.lastName.length > 32) {
      validationErrors.push("Last name must be 32 characters or less");
    }

    // Email validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/; // REGEX for basic email validation
    if (!emailRegex.test(editableUser.value.email)) {
      validationErrors.push("Please enter a valid email address");
    }

    if (editableUser.value.email.length > 100) {
      validationErrors.push("Email must be 100 characters or less");
    }

    if (editableUser.value.phoneNumber.length > 15) {
      validationErrors.push("Phone number must be 15 characters or less");
    }

    // If there are validation errors, show them and stop
    if (validationErrors.length > 0) {
      error.value = validationErrors.join("  •  ");
      return;
    }

    loading.value = true;
    error.value = null;
    const userId = editableUser.value.userId;

    await axios.put(`${API_BASE_URL}/user/${userId}/update`, {
      firstName: editableUser.value.firstName,
      lastName: editableUser.value.lastName,
      email: editableUser.value.email,
      phoneNumber: editableUser.value.phoneNumber,
    }, {
      headers: {
        'Authorization': `Bearer ${authToken.value}`
      }
    });

    // Update successful
    toastMessage.value = 'models.com.stefvisser.springyield.User details updated successfully';
    showToast.value = true;
    isEditing.value = false;

    // Refresh user details to show updated data
    await fetchUserDetails();
  } catch (err) {
    error.value = err.response?.data?.message || 'Failed to update user details';
  } finally {
    loading.value = false;
    setTimeout(() => showToast.value = false, 3000);
  }
}

// Add function to discard changes
const discardChanges = () => {
  // Reset editable user and exit edit mode
  editableUser.value = JSON.parse(JSON.stringify(user.value))
  isEditing.value = false
}

const formatIban = (iban) => {
  if (!iban) return ''
  const clean = iban.replace(/\s/g, '')
  return clean.match(/.{1,4}/g).join(' ')
}

const openDeleteModalHandler = () => { // Renamed to avoid conflict if a variable 'openDeleteModal' was intended
  if (user.value) { // Ensure user is loaded before trying to delete
    showDeleteModal.value = true
  } else {
    toastMessage.value = 'models.com.stefvisser.springyield.User data not loaded yet. Cannot open delete modal.';
    showToast.value = true;
    setTimeout(() => showToast.value = false, 3000);
  }
}

const closeDeleteModalHandler = () => {
  showDeleteModal.value = false
}

const handleUserDeletionSuccess = (deletedUser) => {
  toastMessage.value = `models.com.stefvisser.springyield.User ${deletedUser.firstName} ${deletedUser.lastName} deleted successfully!`
  showToast.value = true
  closeDeleteModalHandler()
  setTimeout(() => {
    showToast.value = false
    router.push({name: 'Users'}) // Navigate back to users list after deletion
  }, 3000)
}

const handleUserDeletionError = (errorMessage) => {
  toastMessage.value = errorMessage
  showToast.value = true
  closeDeleteModalHandler()
  setTimeout(() => {
    showToast.value = false
  }, 3000)
}

const openApproveModalHandler = () => { // Renamed
  if (user.value) { // Ensure user is loaded
    showApproveModal.value = true
  } else {
    toastMessage.value = 'models.com.stefvisser.springyield.User data not loaded yet. Cannot open approve modal.';
    showToast.value = true
    setTimeout(() => showToast.value = false, 3000);
  }
}

const closeApproveModalHandler = () => {
  showApproveModal.value = false
}

const handleUserApprovalSuccess = (approvedUser) => {
  toastMessage.value = `models.com.stefvisser.springyield.User ${approvedUser.firstName} ${approvedUser.lastName} approved successfully!`
  showToast.value = true
  fetchUserDetails() // Refresh user details
  closeApproveModalHandler()
  setTimeout(() => {
    showToast.value = false
  }, 3000)
}

const handleUserApprovalError = (errorMessage) => {
  toastMessage.value = errorMessage
  showToast.value = true
  closeApproveModalHandler()
  setTimeout(() => {
    showToast.value = false
  }, 3000)
}

const goBack = () => {
  router.push({name: "Users"}) // Navigate back to users list
}

onMounted(() => {
  // role.value = localStorage.getItem('role') // Removed, role is computed
  fetchUserDetails()
})

// Watch for route parameter changes to reload user data when navigating between different users
watch(() => route.params.userId, (newUserId, oldUserId) => {
  if (newUserId !== oldUserId) {
    // Reset component state for the new user
    user.value = null
    editableUser.value = null
    isEditing.value = false
    error.value = null

    // Fetch details for the new user ID
    fetchUserDetails()
  }
}, {immediate: false})
</script>


<template>
  <div class="flex min-h-screen bg-gray-100">
    <Sidebar :router="router" :user="storedUser" :role="role"/>
    <Toast :show="showToast" :message="toastMessage"/>
    <div class="flex-1 p-8">
      <div class="mb-6">
        <div class="flex gap-4 items-center mb-6">
          <button @click="goBack"
                  class="px-4 py-2 flex rounded-2xl bg-white hover:bg-gray-100 border-gray-300 border cursor-pointer">
            <img class="pointer-events-none flex items-center text-gray-700"
                 src="/arrow_back_24dp_000.svg"
                 alt="Back-arrow Icon">Back to Users
          </button>
          <div class="flex gap-2">
            <!-- Edit models.com.stefvisser.springyield.User button - visible when not in edit mode -->
            <button v-if="!isEditing && (role === 'EMPLOYEE' || role === 'ADMIN') && user"
                    @click="toggleEditMode"
                    class="flex gap-2 items-center px-4 py-2 rounded-2xl text-white bg-blue-500 hover:bg-blue-600 font-bold border-gray-300 border cursor-pointer"
            >
              <img
                  src="/edit_24dp_FFF.svg"
                  alt="Edit models.com.stefvisser.springyield.User Icon"
                  class="w-5 h-5 flex-shrink-0">
              Edit models.com.stefvisser.springyield.User
            </button>

            <!-- Save button - visible when in edit mode -->
            <button v-if="isEditing"
                    @click="saveChanges"
                    class="flex gap-2 items-center px-4 py-2 rounded-2xl text-white bg-green-500 hover:bg-green-600 font-bold border-gray-300 border cursor-pointer"
            >
              <img
                  src="/edit_24dp_FFF.svg"
                  alt="Save Editing models.com.stefvisser.springyield.User Icon"
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
                  alt="Cancel Editing models.com.stefvisser.springyield.User Icon"
                  class="w-5 h-5 flex-shrink-0">
              Discard Changes
            </button>

            <!-- Delete button - only visible when not in edit mode -->
            <button v-if="!isEditing && (role === 'EMPLOYEE' || role === 'ADMIN') && user"
                    @click="openDeleteModalHandler"
                    class="flex gap-2 items-center px-4 py-2 rounded-2xl text-white bg-red-500 hover:bg-red-600 font-bold border-gray-300 border cursor-pointer"
            >
              <img
                  src="/delete_24dp_FFF.svg"
                  alt="Delete models.com.stefvisser.springyield.User Icon"
                  class="w-5 h-5 flex-shrink-0">
              Delete models.com.stefvisser.springyield.User
            </button>

            <!-- Approve button - only visible when not in edit mode and for unapproved users -->
            <button v-if="!isEditing && (role === 'EMPLOYEE' || role === 'ADMIN') && user && user.role === 'UNAPPROVED'"
                    @click="openApproveModalHandler"
                    class="flex gap-2 items-center px-4 py-2 rounded-2xl text-white bg-green-500 hover:bg-green-600 font-bold border-gray-300 border cursor-pointer"
            >
              <img
                  src="/check_24dp_FFF.svg"
                  alt="Approve models.com.stefvisser.springyield.User Icon"
                  class="w-5 h-5 flex-shrink-0">
              Approve models.com.stefvisser.springyield.User
            </button>
          </div>
        </div>
      </div>

      <div v-if="error" class="mb-4 p-4 font-semibold bg-red-100 text-red-700 rounded">
        {{ error }}
      </div>

      <div v-if="loading" class="text-center py-4">
        Loading...
      </div>

      <div v-else-if="user && editableUser" class="bg-white rounded-lg shadow p-6 overflow-hidden">
        <h1 class="text-2xl font-bold mb-6">models.com.stefvisser.springyield.User Details</h1>
        <div class="space-y-4">
          <div class="grid grid-cols-2 gap-4">
            <div>
              <p class="text-sm text-gray-500">Id</p>
              <p class="font-medium truncate">{{ user.userId }}</p>
            </div>
            <div>
              <p class="text-sm text-gray-500">Name</p>
              <!-- Name field - toggles between display and edit mode -->
              <div v-if="!isEditing" class="font-medium">
                <p class="truncate">{{ user.firstName }} <span v-if="user.firstName.length > 20">{{
                    user.lastName
                  }}</span><span v-else>{{ user.lastName }}</span></p>
              </div>

              <div v-else class="flex gap-2">
                <input type="text" v-model="editableUser.firstName" placeholder="First Name"
                       class="mt-1 block w-full px-3 py-2 bg-white border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-sy-500 focus:border-sy-500 sm:text-sm truncate">
                <input type="text" v-model="editableUser.lastName" placeholder="Last Name"
                       class="mt-1 block w-full px-3 py-2 bg-white border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-sy-500 focus:border-sy-500 sm:text-sm truncate">
              </div>
            </div>

            <div>
              <p class="text-sm text-gray-500">BSN Number</p>
              <p class="font-medium truncate">{{ user.bsnNumber }}</p>
            </div>

            <div>
              <p class="text-sm text-gray-500">Email</p>
              <!-- Email field - toggles between display and edit mode -->
              <div v-if="!isEditing" class="font-medium truncate">{{ user.email }}</div>
              <input v-else type="email" v-model="editableUser.email" placeholder="Email"
                     class="mt-1 block w-full px-3 py-2 bg-white border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-sy-500 focus:border-sy-500 sm:text-sm">
            </div>


            <div>
              <p class="text-sm text-gray-500">Role</p>
              <span
                  :class="{
                      'bg-green-300 text-green-800': user.role === 'APPROVED',
                      'bg-red-300 text-red-800': user.role === 'UNAPPROVED',
                      'bg-sy-300 text-sy-800': user.role === 'EMPLOYEE',
                      'bg-black text-white': user.role === 'ADMIN'
                    }"
                  class="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full"
              >
                    {{ user.role }}
                  </span>
            </div>
            <div>
              <p class="text-sm text-gray-500">Phone Number</p>
              <!-- Phone field - toggles between display and edit mode -->
              <div v-if="!isEditing" class="font-medium truncate">{{ user.phoneNumber }}</div>
              <input v-else type="tel" v-model="editableUser.phoneNumber" placeholder="Phone Number"
                     class="mt-1 block w-full px-3 py-2 bg-white border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-sy-500 focus:border-sy-500 sm:text-sm">
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- Modals -->
  <DeleteUserModal
      :show="showDeleteModal"
      :user="user"
      :spinning="deleteModalSpinning"
      :token="authToken"
      @close="closeDeleteModalHandler"
      @delete-success="handleUserDeletionSuccess"
      @delete-error="handleUserDeletionError"
  />

  <ApproveUserModal
      :show="showApproveModal"
      :user="user"
      :token="authToken"
      @close="closeApproveModalHandler"
      @approve-success="handleUserApprovalSuccess"
      @approve-error="handleUserApprovalError"
  />
</template>

<script setup>
import {onMounted, computed, watch, ref} from 'vue'
import {useRouter, useRoute} from 'vue-router'
import {getUserRole} from '@/utils/auth.js'
import {usePaginatedData} from '@/utils/usePaginatedData.js'
import {API_BASE_URL} from '@/config.js';
import Sidebar from '@/components/adminComps/Sidebar.vue'
import ErrorDisplay from '@/components/adminComps/ErrorDisplay.vue'
import SearchInput from '@/components/adminComps/SearchInput.vue'
import PaginationControls from '@/components/adminComps/PaginationControls.vue'
import PageSection from '@/components/adminComps/PageSection.vue'
import DeleteUserModal from '@/components/modals/DeleteUserModal.vue'
import ApproveUserModal from '@/components/modals/ApproveUserModal.vue'
import {formatNameWithInitial} from '@/utils/formatters.js'

// Computed properties
const router = useRouter()
const route = useRoute()
const storedUser = computed(() => JSON.parse(localStorage.getItem('user')))
const role = computed(() => getUserRole())

const showDeleteModal = ref(false)
const userToDelete = ref(null)
const deleteModalSpinning = ref(true)

const showApproveModal = ref(false)
const userToApprove = ref(null)

const authToken = computed(() => localStorage.getItem('token')) // Get auth token

const {
  items: users,
  loading,
  error,
  searchQuery,
  filters: userFilters,
  currentPage,
  totalItems: totalUsers,
  itemsPerPage: usersPerPage,
  fetchData: fetchUsers,
  goToPreviousPage,
  goToNextPage
} = usePaginatedData({
  apiUrl: `${API_BASE_URL}/user/search`,
  initialFilters: {roleFilter: 'All Users'},
  filterParamNames: {roleFilter: 'role'}
})

// Lifecycle
onMounted(() => {
  // Apply filters from URL query parameters on initial load
  if (route.query.roleFilter)
    userFilters.value.roleFilter = route.query.roleFilter
  else
    fetchUsers()
})

// Watch for query parameter changes to re-apply filters if needed (optional, but good for direct URL changes)
watch(() => route.query.roleFilter, (newRoleFilter) => {
  if (newRoleFilter && newRoleFilter !== userFilters.value.roleFilter) {
    userFilters.value.roleFilter = newRoleFilter
    // fetchData will be called by the watcher in usePaginatedData
  } else if (!newRoleFilter && userFilters.value.roleFilter !== 'All Users') {
    // If query param is removed, reset to default (or fetch all)
    // userFilters.value.roleFilter = 'All Users' // Or your default initial filter
    // fetchData(); // Potentially, or let the watcher in usePaginatedData handle it
  }
}, {immediate: false}) // immediate: false because onMounted handles initial load

const openDeleteModal = (user) => {
  userToDelete.value = user
  showDeleteModal.value = true
}

const closeDeleteModal = () => {
  showDeleteModal.value = false
  userToDelete.value = null
}

// REMOVED confirmDelete function, logic moved to modal

const openApproveModal = (user) => {
  userToApprove.value = user
  showApproveModal.value = true
}

const closeApproveModal = () => {
  showApproveModal.value = false
  userToApprove.value = null
}

// REMOVED confirmApprove function, logic moved to modal

const handleUserDeletionSuccess = (deletedUser) => {
  fetchUsers() // Refresh the list
  closeDeleteModal() // Close modal on success
}

const handleUserDeletionError = (errorMessage) => {
  // Optionally, set an error ref here to display in the UI if needed
  // error.value = errorMessage;
  closeDeleteModal() // Close modal even on error, or handle differently
}

const handleUserApprovalSuccess = (approvedUser) => {
  fetchUsers() // Refresh the list
  closeApproveModal() // Close modal on success
}

const handleUserApprovalError = (errorMessage) => {
  // Optionally, set an error ref here to display in the UI if needed
  // error.value = errorMessage;
  closeApproveModal() // Close modal even on error, or handle differently
}
</script>


<template class="h-dvh flex flex-col">
  <div class="flex min-h-screen bg-gray-100">
    <!-- Sidebar -->
    <sidebar :router="router" :user="storedUser" :role="role"/>

    <!-- Main Content -->
    <main class="flex-1 p-8 flex flex-col h-dvh overflow-hidden">
      <h1 class="text-2xl font-bold text-gray-800 mb-6">User Overview</h1>

      <!-- Error State -->
      <ErrorDisplay :error="error"/>

      <!-- Search and Filter Section -->
      <PageSection title="Search Users">
        <template #header-extra>
          <!-- Role Filter Dropdown -->
          <div class="relative">
            <select
                v-model="userFilters.roleFilter"
                class="appearance-none bg-gray-100 border border-gray-300 rounded-lg px-4 py-2 pr-8 focus:outline-none focus:ring-2 focus:ring-sy-500"
            >
              <option>All Users</option>
              <option value="APPROVED">Approved</option>
              <option value="UNAPPROVED">Unapproved</option>
              <option v-if="role === 'ADMIN'" value="EMPLOYEE">Employee</option>
            </select>
            <img class="pointer-events-none absolute inset-y-2.5 right-0 flex items-center px-2 text-gray-700"
                 src="/keyboard_arrow_down_24dp_000.svg"
                 alt="Dropdown Icon">
          </div>
        </template>
        <SearchInput v-model="searchQuery" placeholder="Search by name, email or BSN"/>
      </PageSection>

      <!-- Users Table -->
      <div class="bg-white rounded-lg shadow flex-1 flex flex-col overflow-hidden">
        <div class="overflow-y-auto flex-grow">
          <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50 sticky top-0 z-10">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                ID
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Full Name
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Email
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                BSN
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Status
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Actions
              </th>
            </tr>
            </thead>

            <tbody class="bg-white divide-y divide-gray-200 ">
            <tr v-for="u in users" :key="u.userId" class="hover:bg-gray-50">
              <!-- Changed user to u to avoid conflict with storedUser -->
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500 max-w-xs truncate">
                {{ u.userId }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900 max-w-xs truncate">
                {{ formatNameWithInitial(u.firstName, u.lastName) }} <!-- Use formatUserName -->
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500 max-w-xs truncate">
                {{ u.email }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500 max-w-xs truncate">
                {{ u.bsnNumber }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap flex gap-2 items-center">
                  <span
                      :class="{
                      'bg-green-300 text-green-800': u.role === 'APPROVED',
                      'bg-red-300 text-red-800': u.role === 'UNAPPROVED',
                      'bg-sy-300 text-sy-800': u.role === 'EMPLOYEE',
                      'bg-black text-white': u.role === 'ADMIN'
                    }"
                      class="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full"
                  >
                    {{ u.role }}
                  </span>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm">
                <div class="flex items-center gap-3">
                  <div class="flex min-w-max">
                    <router-link
                        :to="{
                        name: 'UserDetails',
                        params: { userId: u.userId }
                      }"
                        @click="router.dynamicPageData[`/admin/user/${u.userId}`] = { firstName: u.firstName, lastName: u.lastName }"
                        class="p-3 w-11 rounded-l-2xl bg-blue-500 hover:bg-blue-600 cursor-pointer flex-shrink-0">
                      <img
                          src="/info_24dp_FFF.svg"
                          alt="User Info Icon"
                          class="w-5 h-5 flex-shrink-0">
                    </router-link>
                    <button @click="openDeleteModal(u)"
                            class="p-3 w-11 rounded-r-2xl bg-red-500 hover:bg-red-600 cursor-pointer flex-shrink-0">
                      <img
                          src="/delete_24dp_FFF.svg"
                          alt="User Deletion Icon"
                          class="w-5 h-5 flex-shrink-0">
                    </button>
                  </div>
                  <button v-if="u.role === 'UNAPPROVED'"
                          @click="openApproveModal(u)"
                          class="p-3 w-11 rounded-2xl bg-green-500 hover:bg-green-600 cursor-pointer flex-shrink-0">
                    <img
                        src="/check_24dp_FFF.svg"
                        alt="User Info Icon"
                        class="w-5 h-5 flex-shrink-0">
                  </button>
                </div>
              </td>
            </tr>

            <!-- Empty state -->
            <tr v-if="users.length === 0 && !loading">
              <td colspan="6" class="px-6 py-4 text-center text-sm text-gray-500">
                No users found
              </td>
            </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination -->
        <PaginationControls
            :current-page="currentPage"
            :items-per-page="usersPerPage"
            :total-items="totalUsers"
            @previous-page="goToPreviousPage"
            @next-page="goToNextPage"
        />
      </div>
    </main>
  </div>

  <!-- Delete Confirmation Modal -->
  <DeleteUserModal
      :show="showDeleteModal"
      :user="userToDelete"
      :spinning="deleteModalSpinning"
      :token="authToken"
      @close="closeDeleteModal"
      @delete-success="handleUserDeletionSuccess"
      @delete-error="handleUserDeletionError"
  />

  <!-- Approve Confirmation Modal -->
  <ApproveUserModal
      :show="showApproveModal"
      :user="userToApprove"
      :token="authToken"
      @close="closeApproveModal"
      @approve-success="handleUserApprovalSuccess"
      @approve-error="handleUserApprovalError"
  />
</template>

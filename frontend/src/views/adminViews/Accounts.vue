<script setup>
import {onMounted, computed} from 'vue'
import {useRouter} from 'vue-router'
import {getUserRole} from '@/utils/auth.js'
import {usePaginatedData} from '@/utils/usePaginatedData.js'
import Sidebar from '@/components/adminComps/Sidebar.vue'
import ErrorDisplay from '@/components/adminComps/ErrorDisplay.vue'
import SearchInput from '@/components/adminComps/SearchInput.vue'
import PaginationControls from '@/components/adminComps/PaginationControls.vue'
import PageSection from '@/components/adminComps/PageSection.vue'
import {API_BASE_URL} from '@/config.js';
import {formatNameWithInitial, formatCurrency} from '@/utils/formatters.js'

// Computed properties
const router = useRouter()
const storedUser = computed(() => JSON.parse(localStorage.getItem('user')))
const role = computed(() => getUserRole())

const {
  items: accounts,
  loading,
  error,
  searchQuery,
  filters: accountFilters,
  currentPage,
  totalItems: totalAccounts,
  itemsPerPage: accountsPerPage,
  fetchData: fetchAccounts,
  goToPreviousPage,
  goToNextPage
} = usePaginatedData({
  apiUrl: `${API_BASE_URL}/account/search`,
  initialFilters: {
    accountTypeFilter: '', // Changed from 'All Accounts'
    accountStatusFilter: '' // Changed from 'All Status'
  },
  filterParamNames: {
    accountTypeFilter: 'accountType',
    accountStatusFilter: 'status'
  },

})

// Lifecycle
onMounted(() => {
    fetchAccounts()
})
</script>


<template class="h-dvh flex flex-col">
  <div class="flex min-h-screen bg-gray-100">
    <!-- Sidebar -->
    <sidebar :router="router" :user="storedUser" :role="role"/>

    <!-- Main Content -->
    <main class="flex-1 p-8 flex flex-col h-dvh overflow-hidden">
      <h1 class="text-2xl font-bold text-gray-800 mb-6">Accounts Overview</h1>

      <!-- Error State -->
      <ErrorDisplay :error="error"/>

      <!-- Search and Filter Section -->
      <PageSection title="Search Accounts">
        <template #header-extra>
          <!-- models.com.stefvisser.springyield.Account Type Filter Dropdown -->
          <div class="flex items-center space-x-4">
            <div class="relative">
              <select
                  v-model="accountFilters.accountStatusFilter"
                  class="appearance-none bg-gray-100 border border-gray-300 rounded-lg px-4 py-2 pr-8 focus:outline-none focus:ring-2 focus:ring-sy-500"
              >
                <option value="">All Status</option>
                <option value="ACTIVE">Active</option> <!-- Ensure value matches backend enum/string -->
                <option value="DEACTIVATED">Deactivated</option> <!-- Ensure value matches backend enum/string -->
              </select>
              <img class="pointer-events-none absolute inset-y-2.5 right-0 flex items-center px-2 text-gray-700"
                   src="/keyboard_arrow_down_24dp_000.svg"
                   alt="Dropdown Icon">
            </div>
            <div class="relative">
              <select
                  v-model="accountFilters.accountTypeFilter"
                  class="appearance-none bg-gray-100 border border-gray-300 rounded-lg px-4 py-2 pr-8 focus:outline-none focus:ring-2 focus:ring-sy-500"
              >
                <option value="">All Accounts</option>
                <option value="PAYMENT">Payment</option> <!-- Ensure value matches backend enum/string -->
                <option value="SAVINGS">Savings</option> <!-- Ensure value matches backend enum/string -->
              </select>
              <img class="pointer-events-none absolute inset-y-2.5 right-0 flex items-center px-2 text-gray-700"
                   src="/keyboard_arrow_down_24dp_000.svg"
                   alt="Dropdown Icon">
            </div>
          </div>


        </template>
        <SearchInput v-model="searchQuery" placeholder="Search by IBAN"/>
      </PageSection>

      <!-- Accounts Table -->
      <div class="bg-white rounded-lg shadow flex-1 flex flex-col overflow-hidden">
        <div class="overflow-y-auto flex-grow">
          <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50 sticky top-0 z-10">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                models.com.stefvisser.springyield.Account ID
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Owner
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                IBAN
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Balance
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Type models.com.stefvisser.springyield.Account
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
            <tr v-for="a in accounts" :key="a.accountId" class="hover:bg-gray-50">
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                {{ a.accountId }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                {{ a.user ? formatNameWithInitial(a.user.firstName, a.user.lastName) : 'No Owner (Deleted)' }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                {{ a.iban }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm">
    <span class="inline-block font-semibold text-lg"
          :class="{'text-green-500': a.balance > 0, 'text-black': a.balance === 0, 'text-red-500': a.balance < 0}">
                  {{ formatCurrency(a.balance) }}
                </span>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm">
                <span
                    :class="{
                    'bg-sy-300 text-sy-800': a.accountType === 'PAYMENT',
                    'bg-amber-300 text-amber-800': a.accountType === 'SAVINGS',
                    'bg-black text-white': a.accountType === 'ATM'
                  }"
                    class="px-2 py-1 inline-block text-xs leading-5 font-semibold rounded-full"
                >
                  {{ a.accountType }}
                </span>
              </td>
              <td>
                <span
                   :class="{
                  'px-2 py-1 inline-block text-sm leading-5 font-semibold rounded-full': true,
                  'bg-green-300 text-green-800': a.status === 'ACTIVE',
                  'bg-neutral-800 text-neutral-100': a.status === 'DEACTIVATED',
                }"
                            >
              {{ a.status }}
            </span>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm">
                <div class="flex items-center">
                  <router-link
                      :to="{
                        name: 'AccountDetails',
                        params: { accountId: a.accountId }
                      }"
                      @click="router.dynamicPageData[`/admin/account/${a.accountId}`] = {
                        accountId: a.accountId,
                        firstName: a.user ? a.user.firstName : 'Deleted',
                        lastName: a.user ? a.user.lastName : 'User'
                      }"
                      class="p-3 w-11 rounded-2xl bg-blue-500 hover:bg-blue-600 cursor-pointer flex-shrink-0">
                    <img
                        src="/info_24dp_FFF.svg"
                        alt="models.com.stefvisser.springyield.Account Info Icon"
                        class="w-5 h-5 flex-shrink-0">
                  </router-link>
                </div>
              </td>
            </tr>

            <!-- Empty state -->
            <tr v-if="accounts.length === 0 && !loading">
              <td colspan="5" class="px-6 py-4 text-center text-sm text-gray-500"> <!-- Updated colspan to 5 -->
                No accounts found
              </td>
            </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination -->
        <PaginationControls
            :current-page="currentPage"
            :items-per-page="accountsPerPage"
            :total-items="totalAccounts"
            @previous-page="goToPreviousPage"
            @next-page="goToNextPage"
        />
      </div>
    </main>
  </div>
</template>

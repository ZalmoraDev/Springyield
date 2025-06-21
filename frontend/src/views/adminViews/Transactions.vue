<script setup>
import {onMounted, computed, watch, ref} from 'vue' // Added ref
import {useRouter} from 'vue-router'
import {getUserRole} from '@/utils/auth.js'
import {usePaginatedData} from '@/utils/usePaginatedData.js'
import Sidebar from '@/components/adminComps/Sidebar.vue'
import ErrorDisplay from '@/components/adminComps/ErrorDisplay.vue'
import SearchInput from '@/components/adminComps/SearchInput.vue'
import PaginationControls from '@/components/adminComps/PaginationControls.vue'
import PageSection from '@/components/adminComps/PageSection.vue'
import {API_BASE_URL} from '@/config.js' // Import API_BASE_URL
import {formatCurrency} from '@/utils/formatters.js'

const router = useRouter()
const storedUser = computed(() => JSON.parse(localStorage.getItem('user')))
const role = computed(() => getUserRole())

// Navigate to the transaction creation page
const makeTransaction = () => {
  router.push({name: 'AdminCreateTransaction'});
};

const dateRange = ref({
  startDate: '',
  endDate: ''
})

const amountFilter = ref({
  operator: '',
  amountFrom: ''
});

const filters = ref({
  query: '',
  type: '',
  startDate: '',
  endDate: '',
  amountFrom: null,
  amountTo: null,
  amountOperator: ''
})

const {
  items: transactions,
  loading,
  error,
  searchQuery,
  filters: transactionFilters,
  currentPage,
  totalItems: totalTransactions,
  itemsPerPage: transactionsPerPage,
  fetchData: fetchTransactions,
  goToPreviousPage,
  goToNextPage
} = usePaginatedData({
  apiUrl: `${API_BASE_URL}/transactions/search`,
  initialFilters: {
    selectedTransactionType: '',
    startDate: '',
    endDate: '',
    amountOperator: '',
    amountFrom: '',
    amountTo: ''
  },
  filterParamNames: {
    selectedTransactionType: 'type',
    startDate: 'startDate',
    endDate: 'endDate',
    amountOperator: 'amountOperator',
    amountFrom: 'amountFrom',
    amountTo: 'amountTo'
  }
})

// Update the date range watcher in Transactions.vue
watch(dateRange, (newRange) => {
  // Only filter if both dates are provided
  if (newRange.startDate && newRange.endDate) {
    transactionFilters.value = {
      ...transactionFilters.value,
      startDate: `${newRange.startDate}T00:00:00`,
      endDate: `${newRange.endDate}T23:59:59`
    };
    fetchTransactions();
  } else {
    // Reset date filters if either date is cleared
    transactionFilters.value = {
      ...transactionFilters.value,
      startDate: null,
      endDate: null
    };
    fetchTransactions();
  }
}, {deep: true});

watch(amountFilter, (newFilter) => {
  // Update the filters in a similar way to how search query works
  if (newFilter.operator && newFilter.amountFrom !== '') {
    const amount = parseFloat(newFilter.amountFrom);
    if (!isNaN(amount)) {
      transactionFilters.value = {
        ...transactionFilters.value,
        amountOperator: newFilter.operator,
        amountFrom: amount,
        amountTo: amount
      };
    }
  } else {
    // Reset amount filters when empty
    transactionFilters.value = {
      ...transactionFilters.value,
      amountOperator: null,
      amountFrom: null,
      amountTo: null
    };
  }

  // Trigger the fetch - this will use the repository's searchTransactions method
  fetchTransactions();
}, {deep: true});

// Reset filters function
const resetFilters = () => {
  dateRange.value = {
    startDate: '',
    endDate: ''
  };
  amountFilter.value = {
    operator: '',
    amountFrom: '',
    amountTo: ''
  };
  transactionFilters.value = {
    query: '',
    type: '',
    startDate: null,
    endDate: null,
    amountFrom: null,
    amountTo: null,
    amountOperator: ''
  };
  fetchTransactions();
};

// Fix onMounted
onMounted(() => fetchTransactions())
</script>


<template class="h-dvh flex flex-col">
  <div class="flex min-h-screen bg-gray-100">
    <!-- Sidebar -->
    <sidebar :router="router" :user="storedUser" :role="role"/>

    <!-- Main Content -->
    <main class="flex-1 p-8 flex flex-col h-dvh overflow-hidden">
      <div class="flex justify-between items-center mb-6">
      <h1 class="text-2xl font-bold text-gray-800 mb-6">Transactions Overview</h1>

      <button
          @click="makeTransaction"
          class="px-5 py-3 text-white bg-blue-600 hover:bg-blue-700 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
      >
        Make models.com.stefvisser.springyield.Transaction
      </button>
      </div>

        <!-- Error State -->
      <ErrorDisplay :error="error"/>

      <!-- Search & Filter Section -->
      <PageSection title="Search Transactions">
        <template #header-extra>
          <div class="flex gap-4">
            <div class="flex gap-2 items-center">
              <button
                  @click="resetFilters"
                  class="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                Reset Filters
              </button>
            </div>

            <!-- Existing transaction type dropdown -->
            <select
                v-model="transactionFilters.selectedTransactionType"
                class="p-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-sy-500"
            >
              <option value="">All Types</option>
              <option value="TRANSFER">Transfer</option>
              <option value="DEPOSIT">Deposit</option>
              <option value="WITHDRAW">Withdraw</option>
            </select>

            <!-- Date Range Filter -->
            <div class="flex gap-2 items-center">
              <input
                  type="date"
                  v-model="dateRange.startDate"
                  class="p-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-sy-500"
              />
              <span>to</span>
              <input
                  type="date"
                  v-model="dateRange.endDate"
                  class="p-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-sy-500"
              />
            </div>

            <!-- Amount Filter Section -->
            <div class="flex gap-2 items-center">
              <!-- Amount operator dropdown -->
              <select
                  v-model="amountFilter.operator"
                  class="p-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-sy-500"
              >
                <option value="">Amount Filter</option>
                <option value="lt">Less than</option>
                <option value="gt">Greater than</option>
                <option value="eq">Equal to</option>
              </select>

              <!-- Amount input field (always visible) -->
              <input
                  type="number"
                  v-model="amountFilter.amountFrom"
                  placeholder="Enter amount"
                  class="p-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-sy-500"
                  step="0.01"
              />
            </div>
          </div>
        </template>
        <SearchInput v-model="searchQuery" placeholder="Search by IBAN or reference"/>
      </PageSection>

      <!-- Transactions Table -->
      <div class="bg-white rounded-lg shadow flex-1 flex flex-col overflow-hidden">
        <div class="overflow-y-auto flex-grow">
          <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50 sticky top-0 z-10">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                ID
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                From models.com.stefvisser.springyield.Account
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                To models.com.stefvisser.springyield.Account
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Amount
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Type
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Reference
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Date
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Actions
              </th>
            </tr>
            </thead>

            <tbody class="bg-white divide-y divide-gray-200">
            <tr v-for="t in transactions" :key="t.transactionId" class="hover:bg-gray-50">
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ t.transactionId }}</td>
              <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{{ t.fromAccount }}</td>
              <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{{ t.toAccount }}</td>
              <td class="px-6 py-4 whitespace-nowrap text-sm">
    <span class="inline-block font-semibold text-lg"
          :class="{'text-green-500': t.transferAmount > 0, 'text-black': t.transferAmount === 0, 'text-red-500': t.transferAmount < 0}">
                  {{ formatCurrency(t.transferAmount) }}
                </span>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm">
                <span
                    :class="{
                    'bg-amber-300 text-amber-800': t.transactionType === 'TRANSFER',
                    'bg-red-300 text-red-800': t.transactionType === 'DEPOSIT',
                    'bg-green-300 text-green-800': t.transactionType === 'WITHDRAW'
                  }"
                    class="px-2 py-1 inline-block text-xs leading-5 font-semibold rounded-full"
                >
                  {{ t.transactionType }}
                </span>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ t.reference }}</td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                {{ new Date(t.timestamp).toLocaleString() }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm">
                <div class="flex items-center">
                  <router-link
                      :to="{
                        name: 'TransactionsDetails',
                        params: { transactionId: t.transactionId   }
                      }"
                      @click="router.dynamicPageData[`/admin/transactions/${t.transactionId}`] = { transactionId: t.transactionId }"
                      class="p-3 w-11 rounded-2xl bg-blue-500 hover:bg-blue-600 cursor-pointer flex-shrink-0"
                  >
                    <img
                        src="/info_24dp_FFF.svg"
                        alt="models.com.stefvisser.springyield.Transaction Info Icon"
                        class="w-5 h-5 flex-shrink-0"
                    />
                  </router-link>
                </div>
              </td>
            </tr>

            <!-- Empty state -->
            <tr v-if="transactions.length === 0">
              <td colspan="8" class="px-6 py-4 whitespace-nowrap text-sm text-gray-500 text-center">
                No transactions found.
              </td>
            </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination Controls -->
        <PaginationControls
            :current-page="currentPage"
            :total-items="totalTransactions"
            :items-per-page="transactionsPerPage"
            @previous-page="goToPreviousPage"
            @next-page="goToNextPage"
        />
      </div>
    </main>
  </div>
</template>

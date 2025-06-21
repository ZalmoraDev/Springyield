<script setup>
import {ref, onMounted, watch, computed} from 'vue';
import {useRouter, useRoute} from 'vue-router';
import {getToken} from '@/utils/auth.js';
import InteractiveCard from "@/components/customerComps/InteractiveCard.vue";
import AppHeader from "@/components/customerComps/AppHeader.vue";
import {API_BASE_URL} from '@/config.js';
import {formatCurrency, formatCurrencyNoPlus} from "@/utils/formatters.js"; // <-- Import API_BASE_URL
import apiFetch from '@/utils/api';

const router = useRouter();
const route = useRoute();

// Remove props and directly use a user ref
const user = computed(() => JSON.parse(localStorage.getItem('user')));

// Move the computed property definition here, right after props
const formattedCardHolderName = computed(() => {
  if (!user.value) return 'CARDHOLDER NAME';
  const firstName = user.value.firstName || '';
  const lastName = user.value.lastName || '';
  return `${firstName.charAt(0).toUpperCase()}. ${lastName.toUpperCase()}`;
});

const accountIban = ref(route.params.iban || null); // Initialize from route params
const accountBalance = ref('0.00');
const transactions = ref([]);
const isLoadingTransactions = ref(false);
const accountType = ref(''); // Assuming you might want to display account type later
const searchQuery = ref('');
const transactionFilters = ref({
  selectedTransactionType: ''
});

const dateRange = ref({
  startDate: '',
  endDate: ''
});

const amountFilter = ref({
  operator: '',
  amountFrom: ''
});

const resetFilters = () => {
  searchQuery.value = '';
  transactionFilters.value.selectedTransactionType = '';
  dateRange.value.startDate = '';
  dateRange.value.endDate = '';
  amountFilter.value.operator = '';
  amountFilter.value.amountFrom = '';
};

const filterTransactions = computed(() => {
  if (!transactions.value) return [];

  return transactions.value.filter(t => {
    // Search query filter
    if (searchQuery.value) {
      const searchLower = searchQuery.value.toLowerCase();
      const toAccount = t.toAccount?.toLowerCase() || '';
      const description = t.description?.toLowerCase() || '';

      if (!toAccount.includes(searchLower) && !description.includes(searchLower)) {
        return false;
      }
    }

    // models.com.stefvisser.springyield.Transaction type filter
    if (transactionFilters.value.selectedTransactionType &&
        t.type !== transactionFilters.value.selectedTransactionType) {
      return false;
    }

    // Date range filter
    if (dateRange.value.startDate && t.timestamp &&
        new Date(t.timestamp) < new Date(dateRange.value.startDate)) {
      return false;
    }
    if (dateRange.value.endDate && t.timestamp &&
        new Date(t.timestamp) > new Date(dateRange.value.endDate)) {
      return false;
    }

    // Amount filter
    if (amountFilter.value.operator && amountFilter.value.amountFrom) {
      const amount = Math.abs(t.amount || 0);
      const filterAmount = parseFloat(amountFilter.value.amountFrom);

      if (amountFilter.value.operator === 'lt' && amount >= filterAmount) return false;
      if (amountFilter.value.operator === 'gt' && amount <= filterAmount) return false;
      if (amountFilter.value.operator === 'eq' && amount !== filterAmount) return false;
    }

    return true;
  });
});

const navigateToProfile = () => {
  router.push('/');
};

const navigateToCreateTransaction = () => {
  router.push({name: 'Create models.com.stefvisser.springyield.Transaction', query: {fromAccount: accountIban.value}});
};

// Navigate to ATM page
const navigateToATM = () => {
  router.push({name: 'AtmView', query: {fromAccount: accountIban.value}});
};

const fetchAccountDetails = async (iban) => {
  if (!iban) {

    accountIban.value = "Error: IBAN missing";
    accountBalance.value = '0.00';
    transactions.value = [];
    isLoadingTransactions.value = false; // Reset loading state
    return;
  }
  isLoadingTransactions.value = true;
  transactions.value = [];
  accountIban.value = iban;

  try {
    // Fetch account specific details (like balance)
    const accountResponse = await apiFetch(`/account/iban/${iban}`);
    if (accountResponse.ok) {
      const accountData = await accountResponse.json();
      accountBalance.value = accountData.balance?.toFixed(2) || '0.00';
      accountType.value = accountData.accountType || 'Unknown'; // Assuming accountType is part of the response
    } else {

      accountBalance.value = 'Error';
    }

    // Fetch transactions for this account
    const transResponse = await apiFetch(`/transactions/iban/${iban}`);
    if (transResponse.ok) {
      const rawTransactions = await transResponse.json();
      transactions.value = rawTransactions.map(t => {
        let amount = parseFloat(t.transferAmount);
        let netAmount = (t.toAccount === iban) ? amount : (t.fromAccount === iban) ? -amount : 0;
        let isIncoming = netAmount > 0;
        return {
          id: t.transactionId,
          fromAccount: t.fromAccount,
          toAccount: t.toAccount,
          description: t.description || (isIncoming ? 'Incoming Transfer' : 'Outgoing Transfer'),
          type: t.transactionType,
          timestamp: t.timestamp,
          amount: netAmount
        };
      }).sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp)); // Sort by timestamp descending
    } else {

      transactions.value = []; // Clear transactions on error
    }
  } catch (error) {

    transactions.value = [];
    accountBalance.value = 'Error';
  } finally {
    isLoadingTransactions.value = false;
  }
};



const logout = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
  router.push('/login');
};

onMounted(() => {
  fetchAccountDetails(accountIban.value);
});
</script>

<template>
  <div
      class="account-detail-app bg-gradient-to-b from-neutral-900 to-black min-h-screen flex flex-col text-neutral-300">
    <AppHeader :user="user" @logout="logout" @navigateToProfile="navigateToProfile"/>
    <main class="flex-1 p-8 flex flex-row items-start overflow-y-auto space-x-8">
      <!-- Left Column: Personal Info (Top) and Card (Bottom) -->
      <div class="flex flex-col w-1/4 h-[calc(100vh-145px)]">
        <div class="w-full max-w-xl transform scale-100 mb-8">
          <div class="flex justify-center items-center content-center w-full h-full">
            <InteractiveCard
                :cardNumber="accountIban"
                :cardHolderName="formattedCardHolderName"
                :display-name="accountType == 'SAVINGS' ? 'SpringSave' : 'SpringSpend'"
                :card-type="accountType == 'SAVINGS' ? '/card_yellow.png' : '/card_blue.png'"
            />
          </div>
        </div>

        <div class="flex-grow min-h-0">
          <div class="bg-[#191B1D] p-6 rounded-[30px] shadow-lg w-full max-w-full overflow-y-auto h-full">
            <h3 class="text-2xl font-semibold text-neutral-100 mb-4">Personal Information</h3>
            <div class="space-y-2 text-neutral-300" v-if="user">
              <div>
                <p><strong>Name:</strong></p>
                <p class="truncate"> {{ user.firstName + ' ' + user.lastName }}</p>
              </div>
              <div>
                <p><strong>Email:</strong></p>
                <p class="truncate"> {{ user.email }}</p>
              </div>
              <div>
                <p><strong>Phone:</strong></p>
                <p class="truncate"> {{ user.phoneNumber || 'N/A' }}</p>
              </div>
              <div>
                <p><strong>BSN:</strong></p>
                <p class="truncate"> {{ user.bsnNumber || 'N/A' }}</p>
              </div>
            </div>
            <p v-else class="text-neutral-400">Loading user information...</p>
          </div>
        </div>
      </div>

      <!-- Right Column: models.com.stefvisser.springyield.Account Details and Transactions (Static Panel) -->
      <div class="flex flex-col w-3/4">
        <div
            class="bg-[#191B1D] text-neutral-300 p-6 md:p-8 rounded-[30px] shadow-2xl w-full max-w-full h-[calc(100vh-145px)] flex flex-col">
          <div class="flex flex-col justify-between items-start mb-6">
            <h2 class="text-3xl font-bold text-blue-300 mb-2">models.com.stefvisser.springyield.Account: {{ accountIban || 'Loading...' }}</h2>
            <p :class="accountBalance < 0 ? 'text-red-400' : 'text-white'" class="text-xl font-semibold">Balance:
              {{ formatCurrencyNoPlus(parseFloat(accountBalance)) }}</p>
          </div>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
            <button @click="navigateToCreateTransaction"
                    class="bg-neutral-600 hover:bg-neutral-500 text-white font-semibold py-3 px-4 rounded-[10px] shadow-md transition-colors duration-200 cursor-pointer">
              Create New models.com.stefvisser.springyield.Transaction
            </button>
            <button @click="navigateToATM"
                    class="bg-neutral-600 hover:bg-neutral-500 text-white font-semibold py-3 px-4 rounded-[10px] shadow-md transition-colors duration-200 cursor-pointer">
              ATM
            </button>
          </div>
          <h3 class="text-2xl font-semibold text-neutral-100 mb-4">Latest Transactions</h3>

          <!-- Filters Section -->
          <div class="flex flex-wrap gap-4 mb-4">
            <div class="flex gap-2 items-center">
              <button
                  @click="resetFilters"
                  class="px-4 py-2 text-sm font-medium text-white bg-neutral-600 hover:bg-neutral-500 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                Reset Filters
              </button>
            </div>

            <select
                v-model="transactionFilters.selectedTransactionType"
                class="p-2 text-sm bg-neutral-700 border border-neutral-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-white"
            >
              <option value="">All Types</option>
              <option value="TRANSFER">Transfer</option>
              <option value="DEPOSIT">Deposit</option>
              <option value="WITHDRAW">Withdraw</option>
            </select>

            <div class="flex gap-2 items-center">
              <input
                  type="date"
                  v-model="dateRange.startDate"
                  class="p-2 text-sm bg-neutral-700 border border-neutral-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-white"
              />
              <span>to</span>
              <input
                  type="date"
                  v-model="dateRange.endDate"
                  class="p-2 text-sm bg-neutral-700 border border-neutral-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-white"
              />
            </div>

            <div class="flex gap-2 items-center">
              <select
                  v-model="amountFilter.operator"
                  class="p-2 text-sm bg-neutral-700 border border-neutral-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-white"
              >
                <option value="">Amount Filter</option>
                <option value="lt">Less than</option>
                <option value="gt">Greater than</option>
                <option value="eq">Equal to</option>
              </select>
              <input
                  type="number"
                  v-model="amountFilter.amountFrom"
                  placeholder="Enter amount"
                  class="p-2 text-sm bg-neutral-700 border border-neutral-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-white"
                  step="0.01"
              />
            </div>

            <input
                type="text"
                v-model="searchQuery"
                placeholder="Search transactions..."
                class="p-2 text-sm bg-neutral-700 border border-neutral-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-white"
            />
          </div>

          <!-- Transactions List -->
          <div class="overflow-y-auto flex-grow border border-neutral-700 rounded-[10px] p-1 bg-black/50">
            <ul v-if="!isLoadingTransactions && filterTransactions.length > 0" class="space-y-3 p-3">
              <li v-for="transaction in filterTransactions" :key="transaction.id"
                  class="flex justify-between items-center p-3 bg-neutral-800 rounded-md hover:bg-neutral-700 transition-colors duration-150">
                <div>
                  <p class="font-medium text-neutral-100">{{ transaction.description }}</p>
                  <p class="text-sm text-neutral-400">{{ transaction.toAccount }}</p>
                  <p class="text-sm text-neutral-400">{{ new Date(transaction.timestamp).toLocaleDateString() }} -
                    {{ new Date(transaction.timestamp).toLocaleTimeString() }}</p>
                </div>
                <p :class="transaction.amount < 0 ? 'text-red-400' : 'text-green-400'" class="font-semibold text-md">
                  {{ formatCurrency(transaction.amount) }}
                </p>
              </li>
            </ul>
            <p v-else-if="isLoadingTransactions" class="text-neutral-400 text-center py-10">Loading transactions...</p>
            <p v-else class="text-neutral-400 text-center py-10">No transactions to display for this account.</p>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>



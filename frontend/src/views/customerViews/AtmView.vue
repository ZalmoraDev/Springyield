<template>
  <div class="atm-view bg-gradient-to-b from-neutral-900 to-black min-h-screen flex flex-col text-neutral-300">


    <main class="flex-1 p-8 flex flex-col items-center justify-start overflow-y-auto">
      <!-- Increased width of the main ATM service content area -->
      <div class="bg-[#191B1D] p-8 rounded-[30px] shadow-xl w-full max-w-xl md:max-w-2xl">
        <!-- Header with title and home button -->
        <div class="flex justify-between items-center mb-10">
          <h2 class="text-3xl md:text-4xl font-bold text-neutral-100">ATM Service</h2>
          <button
              @click="router.push('/')"
              class="flex items-center gap-2 bg-neutral-700 hover:bg-neutral-600 text-neutral-200 font-medium py-2 px-4 rounded-lg transition-colors duration-200"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
              <path
                  d="M10.707 2.293a1 1 0 00-1.414 0l-7 7a1 1 0 001.414 1.414L4 10.414V17a1 1 0 001 1h2a1 1 0 001-1v-2a1 1 0 011-1h2a1 1 0 011 1v2a1 1 0 001 1h2a1 1 0 001-1v-6.586l.293.293a1 1 0 001.414-1.414l-7-7z"/>
            </svg>
            <span>Home</span>
          </button>
        </div>

        <!-- Account Selection -->
        <div class="mb-8">
          <label for="selectedAccount" class="block text-base font-medium text-neutral-300 mb-2">Select Account
            (IBAN)</label>
          <select id="selectedAccount" v-model="selectedAccountIban" @change="handleAccountChange"
                  class="bg-neutral-700 border border-neutral-600 text-neutral-200 text-lg rounded-lg focus:ring-sky-500 focus:border-sky-500 block w-full p-3 placeholder-neutral-400">
            <option disabled value="">Please select an account</option>
            <option v-for="account in userAccounts" :key="account.iban" :value="account.iban">
              {{ account.iban }} ({{ formatAccountType(account.accountType) }})
            </option>
          </select>
        </div>

        <!-- Account Balance -->
        <div v-if="selectedAccountIban && !isLoadingBalance" class="mb-10 p-6 bg-neutral-800 rounded-lg text-center">
          <p class="text-lg text-neutral-400">Current Balance</p>
          <p class="text-4xl md:text-5xl font-bold text-neutral-100 mt-1">&euro;{{
              currentBalance?.toFixed(2) || '0.00'
            }}</p>
        </div>
        <div v-if="isLoadingBalance" class="text-center py-6">
          <p class="text-lg text-neutral-400">Loading balance...</p>
        </div>

        <!-- ATM Actions -->
        <div v-if="selectedAccountIban" class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
          <button @click="showOperationModal('DEPOSIT')"
                  class="bg-green-600 hover:bg-green-700 text-white font-semibold py-5 px-4 rounded-lg shadow-md transition-colors duration-200 text-xl">
            Deposit
          </button>
          <button @click="showOperationModal('WITHDRAW')"
                  class="bg-orange-600 hover:bg-orange-700 text-white font-semibold py-5 px-4 rounded-lg shadow-md transition-colors duration-200 text-xl">
            Withdraw
          </button>
        </div>

        <div v-if="errorMessage" class="mt-6 mb-4 p-3 bg-red-500/20 text-red-300 rounded-lg text-sm">
          {{ errorMessage }}
        </div>
        <div v-if="successMessage" class="mt-6 mb-4 p-3 bg-green-500/20 text-green-300 rounded-lg text-sm">
          {{ successMessage }}
        </div>
      </div>

      <!-- Operation Modal -->
      <div v-if="showModal" class="fixed inset-0 bg-black bg-opacity-75 flex items-center justify-center z-50 p-4">
        <div class="bg-[#1E2225] p-6 sm:p-10 rounded-xl shadow-2xl w-full max-w-lg lg:max-w-xl">
          <h3 class="text-3xl sm:text-4xl font-bold text-neutral-100 mb-8 text-center">{{ currentOperationType }}</h3>

          <!-- Amount Display - Increased size -->
          <div
              class="mb-8 p-4 bg-neutral-800 rounded-lg text-right text-4xl sm:text-5xl font-mono text-neutral-100 min-h-[60px] sm:min-h-[70px]">
            {{ formattedOperationAmount }}
          </div>

          <!-- On-screen Number Pad - Increased size -->
          <div class="grid grid-cols-3 gap-3 sm:gap-4 mb-8">
            <button v-for="digit in [1, 2, 3, 4, 5, 6, 7, 8, 9, 'CLEAR', 0, 'DEL']"
                    :key="digit"
                    @click="handleNumpadInput(digit)"
                    class="py-5 sm:py-6 px-2 text-2xl sm:text-3xl font-semibold rounded-lg transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-sky-500"
                    :class="(typeof digit === 'number') ? 'bg-neutral-700 hover:bg-neutral-600 text-white' : 'bg-amber-600 hover:bg-amber-700 text-white'">
              {{ digit }}
            </button>
          </div>

          <div v-if="modalErrorMessage" class="mb-6 p-3 bg-red-500/20 text-red-300 rounded-lg text-sm">
            {{ modalErrorMessage }}
          </div>

          <form @submit.prevent="handleOperationSubmit">
            <input type="hidden" :value="operationAmount">

            <!-- Confirm/Cancel Buttons - Increased size -->
            <div
                class="flex flex-col sm:flex-row justify-around items-center mt-10 space-y-4 sm:space-y-0 sm:space-x-4">
              <button type="button" @click="closeModal"
                      class="w-full sm:w-auto bg-neutral-700 hover:bg-neutral-600 text-white font-semibold py-4 px-10 text-lg rounded-lg shadow-md transition-colors duration-200">
                Cancel
              </button>
              <button type="submit"
                      :disabled="isSubmittingOperation || operationAmount === '' || operationAmount === '0'"
                      class="w-full sm:w-auto bg-sky-600 hover:bg-sky-700 disabled:bg-sky-800 disabled:cursor-not-allowed text-white font-semibold py-4 px-10 text-lg rounded-lg shadow-md transition-colors duration-200">
                <span v-if="isSubmittingOperation">Processing...</span>
                <span v-else>Confirm {{ currentOperationType }}</span>
              </button>
            </div>
          </form>
        </div>
      </div>
    </main>
  </div>
</template>


<script setup>
import {ref, onMounted, computed} from 'vue';
import {useRouter} from 'vue-router';
import {getToken} from '@/utils/auth.js';
import AppHeader from '@/components/customerComps/AppHeader.vue';
import {API_BASE_URL} from '@/config.js'; // <-- Import API_BASE_URL
import apiFetch from '@/utils/api';

const router = useRouter();
const currentUser = ref(null);
const userAccounts = ref([]);
const selectedAccountIban = ref('');
const currentBalance = ref(null);
const isLoadingAccounts = ref(false);
const isLoadingBalance = ref(false);

const showModal = ref(false);
const currentOperationType = ref('DEPOSIT'); // DEPOSIT or WITHDRAW
const operationAmount = ref(''); // Store as string for numpad input
const isSubmittingOperation = ref(false);

const errorMessage = ref('');
const successMessage = ref('');
const modalErrorMessage = ref('');

const formatAccountType = (type) => {
  if (!type) return 'Unknown';
  return type.toLowerCase().replace('_', ' ');
};

const formattedOperationAmount = computed(() => {
  if (operationAmount.value === '' || operationAmount.value === null) return '€0.00';
  const num = parseInt(operationAmount.value);
  return `€${(num / 100).toFixed(2)}`;
});

const handleNumpadInput = (input) => {
  modalErrorMessage.value = ''; // Clear error on new input
  if (typeof input === 'number') {
    if (operationAmount.value.length < 8) { // Limit input length (e.g., 8 digits for cents)
      if (operationAmount.value === '0' && input !== 0) {
        operationAmount.value = String(input);
      } else if (operationAmount.value === '' && input === 0) {
        operationAmount.value = '0';
      } else {
        operationAmount.value += String(input);
      }
    }
  } else if (input === 'CLEAR') {
    operationAmount.value = '';
  } else if (input === 'DEL') {
    operationAmount.value = operationAmount.value.slice(0, -1);
  }
};

const fetchUserAndAccounts = async () => {
  const userFromStorage = localStorage.getItem('user');
  if (userFromStorage) {
    currentUser.value = JSON.parse(userFromStorage);
  }
  if (!currentUser.value || !currentUser.value.userId) {
    errorMessage.value = 'User not found. Please log in again.';
    return;
  }

  isLoadingAccounts.value = true;
  try {
    const response = await apiFetch(`/user/${currentUser.value.userId}`);
    if (response.ok) {
      const data = await response.json();
      userAccounts.value = data.accounts.filter(acc => acc.accountType === 'PAYMENT' || acc.accountType === 'SAVINGS');
      if (userAccounts.value.length > 0 && !selectedAccountIban.value) {
        // Optionally pre-select first account and fetch its balance
      }
    } else {
      errorMessage.value = 'Failed to fetch user accounts.';
    }
  } catch (error) {
    errorMessage.value = 'Error fetching user accounts.';
  } finally {
    isLoadingAccounts.value = false;
  }
};

const fetchAccountBalance = async (iban) => {
  if (!iban) return;
  isLoadingBalance.value = true;
  try {
    const response = await apiFetch(`/account/iban/${iban}`);
    if (response.ok) {
      const data = await response.json();
      currentBalance.value = data.balance;
    } else {
      errorMessage.value = `Failed to fetch balance for ${iban}.`;
      currentBalance.value = null;
    }
  } catch (error) {
    errorMessage.value = `Error fetching balance for ${iban}.`;
    currentBalance.value = null;
  } finally {
    isLoadingBalance.value = false;
  }
};

const handleAccountChange = () => {
  successMessage.value = '';
  errorMessage.value = '';
  if (selectedAccountIban.value) {
    fetchAccountBalance(selectedAccountIban.value);
  }
};

const showOperationModal = (type) => {
  currentOperationType.value = type;
  operationAmount.value = '';
  modalErrorMessage.value = '';
  successMessage.value = '';
  errorMessage.value = '';
  showModal.value = true;
};

const closeModal = () => {
  showModal.value = false;
};

const handleOperationSubmit = async () => {
  const amountInCents = parseInt(operationAmount.value);
  if (isNaN(amountInCents) || amountInCents <= 0) {
    modalErrorMessage.value = 'Please enter a valid amount greater than zero.';
    return;
  }

  const actualAmount = amountInCents / 100;

  if (currentOperationType.value === 'WITHDRAW' && currentBalance.value < actualAmount) {
    modalErrorMessage.value = 'Insufficient balance for this withdrawal.';
    return;
  }

  isSubmittingOperation.value = true;
  modalErrorMessage.value = '';
  successMessage.value = '';
  errorMessage.value = '';

  const payload = {
    fromAccount: selectedAccountIban.value,
    transferAmount: actualAmount,
    description: `ATM ${currentOperationType.value}`,
    transactionType: currentOperationType.value
  };

  try {
    const response = await apiFetch(`/transactions/atm`, {
      method: 'POST',
      body: payload
    });

    if (response.ok) {
      const result = await response.json();
      successMessage.value = `${currentOperationType.value} of €${actualAmount.toFixed(2)} successful.`;
      fetchAccountBalance(selectedAccountIban.value);
      closeModal();
      operationAmount.value = '';
    } else {
      const errorData = await response.json();
      errorMessage.value = errorData.message || `Failed to process ${currentOperationType.value.toLowerCase()}.`;
      closeModal();
    }
  } catch (error) {
    errorMessage.value = `An error occurred during ${currentOperationType.value.toLowerCase()}.`;
    closeModal();
  } finally {
    isSubmittingOperation.value = false;
  }
};

onMounted(() => {
  fetchUserAndAccounts();
});

</script>

<style scoped>
/* Styles for ATMView */
.min-h-\[60px\] {
  min-height: 60px;
}

.min-h-\[70px\] {
  min-height: 70px;
}

/* Ensure the main view allows for the modal to overlay correctly */
.atm-view {
  position: relative; /* If not already set by flex structure */
}
</style>


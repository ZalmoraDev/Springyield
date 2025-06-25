<template>
  <div class="create-transaction-view bg-gradient-to-b from-neutral-900 to-black min-h-screen flex flex-col text-neutral-300">
    <AppHeader :user="currentUser" />

    <main class="flex-1 p-8 flex justify-center items-start overflow-y-auto">
      <div class="bg-[#191B1D] p-8 rounded-[30px] shadow-xl w-full max-w-2xl min-h-[500px]">
        <template v-if="!showSuccessAnimation">
          <h2 class="text-3xl font-bold text-neutral-100 mb-8 text-center">Create New Transaction</h2>
          <form @submit.prevent="handleTransactionSubmit">
            <div class="mb-6">
              <label for="fromAccount" class="block text-sm font-medium text-neutral-300 mb-1">From Account (IBAN)</label>
              <select id="fromAccount" v-model="transactionForm.fromAccount" class="bg-neutral-700 border border-neutral-600 text-neutral-200 text-sm rounded-lg focus:ring-sky-500 focus:border-sky-500 block w-full p-2.5 placeholder-neutral-400">
                <option disabled value="">Please select an account</option>
                <option v-for="account in userAccounts" :key="account.iban" :value="account.iban">
                  {{ account.iban }} (Balance: &euro;{{ account.balance?.toFixed(2) }})
                </option>
              </select>
              <p v-if="selectedFromAccountBalance !== null" class="text-xs text-neutral-400 mt-1">
                Selected account balance: &euro;{{ selectedFromAccountBalance?.toFixed(2) }}
              </p>
            </div>

            <div class="mb-6">
              <label for="toAccount" class="block text-sm font-medium text-neutral-300 mb-1">To Account (IBAN)</label>
              <div class="relative flex items-stretch">
                <input
                    type="text"
                    id="toAccount"
                    v-model.trim="transactionForm.toAccount"
                    class="bg-neutral-700 border border-neutral-600 text-neutral-200 text-sm rounded-l-lg focus:ring-sky-500 focus:border-sky-500 block w-full p-2.5 placeholder-neutral-400 focus:z-10"
                    placeholder="NLXXBANK0123456789"
                    required
                />
                <button
                    type="button"
                    @click="toggleAddressBook"
                    title="Open Address Book"
                    class="bg-neutral-600 hover:bg-neutral-500 text-neutral-300 px-3 py-2.5 rounded-r-lg border border-l-0 border-neutral-600 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-sky-500 focus:z-10 flex items-center"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M12 6.042A8.967 8.967 0 006 3.75c-1.052 0-2.062.18-3 .512v14.25A8.987 8.987 0 016 18c2.305 0 4.408.867 6 2.292m0-14.25a8.966 8.966 0 016-2.292c1.052 0 2.062.18 3 .512v14.25A8.987 8.987 0 0018 18a8.967 8.967 0 00-6 2.292m0-14.25v14.25" />
                  </svg>
                </button>
                <!-- AddressBook Popup -->
                <AddressBook
                    v-if="!isLoadingAccounts"
                    :userAccounts="userAccounts"
                    :visible="showAddressBook"
                    @select-contact="selectContactFromAddressBook"
                    @close="showAddressBook = false"
                    class="absolute top-0 left-full ml-2 z-20 bg-[#25282a] p-4 rounded-lg shadow-xl border border-neutral-700 w-96 max-h-[400px] overflow-y-auto"
                    style="min-height: 200px;"
                />
              </div>
            </div>

            <div class="mb-6">
              <label for="transferAmount" class="block text-sm font-medium text-neutral-300 mb-1">Amount (&euro;)</label>
              <input type="number" id="transferAmount" v-model.number="transactionForm.transferAmount" min="0.01" step="0.01" class="bg-neutral-700 border border-neutral-600 text-neutral-200 text-sm rounded-lg focus:ring-sky-500 focus:border-sky-500 block w-full p-2.5 placeholder-neutral-400" placeholder="100.00" required>
            </div>

            <div class="mb-8">
              <label for="description" class="block text-sm font-medium text-neutral-300 mb-1">Description</label>
              <textarea id="description" v-model.trim="transactionForm.description" rows="3" class="bg-neutral-700 border border-neutral-600 text-neutral-200 text-sm rounded-lg focus:ring-sky-500 focus:border-sky-500 block w-full p-2.5 placeholder-neutral-400" placeholder="Payment for goods/services"></textarea>
            </div>

            <div v-if="errorMessage" class="mb-4 p-3 bg-red-500/20 text-red-300 rounded-lg">
              {{ errorMessage }}
            </div>

            <div class="flex justify-center">
              <button type="submit" :disabled="isSubmitting" class="bg-sky-600 hover:bg-sky-700 disabled:bg-sky-800 text-white font-semibold py-3 px-8 rounded-lg shadow-md transition-colors duration-200">
                <span v-if="isSubmitting">Processing...</span>
                <span v-else>Send Transaction</span>
              </button>
            </div>
          </form>
        </template>
        <template v-else>
          <div class="flex flex-col items-center justify-center h-full text-center">
            <div class="my-4 w-48 h-48  flex items-center justify-center text-neutral-400">
              <DotLottieVue style="height: 500px; width: 500px" autoplay loop src="https://lottie.host/eb18e6ec-28aa-4df3-8c1b-b662c68f4e17/LJgRjMULLc.lottie" />
            </div>
            <div v-if="successMessage" class="mb-6 p-4 flex flex-col rounded-lg text-lg">
              <p class="text-green-400">Sent!</p>
              <p class="text-white">{{ successMessage }}</p>

            </div>
            <button @click="router.push('/')" class="mt-8 bg-sky-600 hover:bg-sky-700 text-white font-semibold py-3 px-8 rounded-lg shadow-md transition-colors duration-200">
              Go to Dashboard
            </button>
          </div>
        </template>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import AppHeader from '@/components/customerComps/AppHeader.vue';
import AddressBook from '@/components/modals/AddressBook.vue'; // Ensure this path is correct
import apiFetch from '@/utils/api';
import { DotLottieVue } from '@lottiefiles/dotlottie-vue'
const router = useRouter();
const route = useRoute();
const currentUser = ref(null);
const userAccounts = ref([]);
const isLoadingAccounts = ref(false);

const transactionForm = ref({
  fromAccount: '',
  toAccount: '',
  transferAmount: '', // Will be null if input is empty due to .number modifier
  description: ''
});



const isSubmitting = ref(false);
const errorMessage = ref('');
const successMessage = ref('');
const showSuccessAnimation = ref(false);
const showAddressBook = ref(false); // For AddressBook visibility

const selectedFromAccountBalance = computed(() => {
  const selected = userAccounts.value.find(acc => acc.iban === transactionForm.value.fromAccount);
  return selected ? selected.balance : null;
});

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
  errorMessage.value = '';

  try {
    const response = await apiFetch(`/user/${currentUser.value.userId}`);
    if (response.ok) {
      const data = await response.json();
      userAccounts.value = data.accounts;

      const fromAccountQuery = route.query.fromAccount;
      if (fromAccountQuery) {
        const accountToSelect = userAccounts.value.find(acc => acc.iban === fromAccountQuery);
        if (accountToSelect) {
          transactionForm.value.fromAccount = fromAccountQuery;

        } else {


          if (userAccounts.value.length > 0 && !transactionForm.value.fromAccount) {

          }
        }
      } else if (userAccounts.value.length > 0 && !transactionForm.value.fromAccount) {

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

const handleTransactionSubmit = async () => {
  isSubmitting.value = true;
  errorMessage.value = '';

  if (!transactionForm.value.fromAccount || !transactionForm.value.toAccount || !transactionForm.value.transferAmount) {
    errorMessage.value = 'Please fill in all required fields: From Account, To Account, and Amount.';
    isSubmitting.value = false;
    return;
  }

  if (transactionForm.value.fromAccount === transactionForm.value.toAccount) {
    errorMessage.value = 'From and To accounts cannot be the same.';
    isSubmitting.value = false;
    return;
  }

  if (transactionForm.value.transferAmount <= 0) {
    errorMessage.value = 'Amount must be greater than zero.';
    isSubmitting.value = false;
    return;
  }

  const fromAccountDetails = userAccounts.value.find(acc => acc.iban === transactionForm.value.fromAccount);
  if (fromAccountDetails && fromAccountDetails.balance < transactionForm.value.transferAmount) {
    errorMessage.value = 'Insufficient balance in the selected account.';
    isSubmitting.value = false;
    return;
  }

  try {
    const payload = {
      fromAccount: transactionForm.value.fromAccount,
      toAccount: transactionForm.value.toAccount,
      transferAmount: transactionForm.value.transferAmount,
      description: transactionForm.value.description,
      transactionType: 'TRANSFER'
    };

    const response = await apiFetch(`/transactions/create`, {
      method: 'POST',
      body: payload
    });

    if (response.ok) {
      const result = await response.json();
      successMessage.value = `Your transaction has been sent successfully!`;
      showSuccessAnimation.value = true;
      fetchUserAndAccounts();
    } else {
      // Get the error text directly from the response
      const errorText = await response.text();
      try {
        // Try to parse as JSON first (in case it's a structured error response)
        const errorObj = JSON.parse(errorText);
        errorMessage.value = errorObj.message || errorObj.error || errorObj.detail || errorText;
      } catch (e) {
        // If parsing fails, use the raw text
        errorMessage.value = errorText;
      }
      showSuccessAnimation.value = false;
    }
  } catch (error) {
    errorMessage.value = error.message || 'An error occurred while submitting the transaction. Please check your connection.';
    showSuccessAnimation.value = false;
  } finally {
    isSubmitting.value = false;
  }
};


const selectContactFromAddressBook = (contact) => {
  transactionForm.value.toAccount = contact.iban;
  showAddressBook.value = false; // Close address book after selection
};

const prepareNewTransaction = () => {
  showSuccessAnimation.value = false;
  successMessage.value = '';
  errorMessage.value = '';
  showAddressBook.value = false; // Ensure address book is closed

  const fromAccountQuery = route.query.fromAccount;
  let initialFromAccount = '';
  if (fromAccountQuery && userAccounts.value.some(acc => acc.iban === fromAccountQuery)) {
    initialFromAccount = fromAccountQuery;
  }

  transactionForm.value = {
    fromAccount: initialFromAccount,
    toAccount: '',
    transferAmount: null,
    description: ''
  };
};

const toggleAddressBook = () => {
  showAddressBook.value = !showAddressBook.value;
};

onMounted(() => {
  fetchUserAndAccounts();
});
</script>

<style scoped>
/* Add any specific styles if needed */
.min-h-\[500px\] {
  min-height: 500px;
}
</style>
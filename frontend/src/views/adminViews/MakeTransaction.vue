<script setup>
import {ref, computed, onMounted} from 'vue';
import {useRouter} from 'vue-router';
import AddressBook from '@/components/modals/AddressBook.vue';
import apiFetch from '@/utils/api';
import Sidebar from "@/components/adminComps/Sidebar.vue";
import {getUserRole} from "@/utils/auth.js";

const router = useRouter()
const storedUser = computed(() => JSON.parse(localStorage.getItem('user')))
const role = computed(() => getUserRole())

const selectedFromAccountBalance = ref(null);

const transactionForm = ref({
  fromAccount: '',
  toAccount: '',
  transferAmount: '',
  description: ''
});

const resetTransaction = () => {
  // Reset form data
  transactionForm.value = {
    fromAccount: '',
    toAccount: '',
    transferAmount: '',
    description: ''
  };
  // Reset states
  showSuccessAnimation.value = false;
  successMessage.value = '';
  errorMessage.value = '';
  selectedFromAccountBalance.value = null;
};

const isSubmitting = ref(false);
const errorMessage = ref('');
const successMessage = ref('');
const showSuccessAnimation = ref(false);
const showFromAddressBook = ref(false);
const showToAddressBook = ref(false);

// Initialize user data
onMounted(() => {
  const userFromStorage = localStorage.getItem('user');
  if (userFromStorage) {
    storedUser.value = JSON.parse(userFromStorage);
    role.value = storedUser.value.role || 'EMPLOYEE';
  }
});

// Toggle address book visibility
const toggleFromAddressBook = () => {
  showFromAddressBook.value = !showFromAddressBook.value;
  showToAddressBook.value = false; // Close the other one
};

const toggleToAddressBook = () => {
  showToAddressBook.value = !showToAddressBook.value;
  showFromAddressBook.value = false; // Close the other one
};

// models.com.stefvisser.springyield.Account selection handlers
const selectFromAccount = async (contact) => {
  transactionForm.value.fromAccount = contact.iban;
  showFromAddressBook.value = false;

  // Fetch account balance for the selected "from" account
  const response = await apiFetch(`/account/iban/${contact.iban}`);
  if (response.ok) {
    const accountData = await response.json();
    selectedFromAccountBalance.value = accountData.balance;
  }

};

const selectToAccount = (contact) => {
  transactionForm.value.toAccount = contact.iban;
  showToAddressBook.value = false;
};

// Form validation and submission
const handleTransactionSubmit = async () => {
  isSubmitting.value = true;
  errorMessage.value = '';

  // Basic validation
  if (!transactionForm.value.fromAccount || !transactionForm.value.toAccount || !transactionForm.value.transferAmount) {
    errorMessage.value = 'Please fill in all required fields: From models.com.stefvisser.springyield.Account, To models.com.stefvisser.springyield.Account, and Amount.';
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

  // Check sufficient balance if we have the balance info
  if (selectedFromAccountBalance.value !== null && selectedFromAccountBalance.value < transactionForm.value.transferAmount) {
    errorMessage.value = 'Insufficient balance in the selected account.';
    isSubmitting.value = false;
    return;
  }

  try {
    const payload = {
      fromAccount: transactionForm.value.fromAccount,
      toAccount: transactionForm.value.toAccount,
      transferAmount: transactionForm.value.transferAmount,
      description: transactionForm.value.description || '',
      transactionType: 'TRANSFER'
    };

    const response = await apiFetch(`/transactions/create`, {
      method: 'POST',
      body: payload
    });

    if (response.ok) {
      const result = await response.json();
      successMessage.value = `models.com.stefvisser.springyield.Transaction completed successfully! Amount: €${transactionForm.value.transferAmount.toFixed(2)}`;
      showSuccessAnimation.value = true;

      // Reset form
      transactionForm.value = {
        fromAccount: '',
        toAccount: '',
        transferAmount: '',
        description: ''
      };
      selectedFromAccountBalance.value = null;
    } else {
      const errorData = await response.json().catch(() => ({message: 'models.com.stefvisser.springyield.Transaction failed. Please try again.'}));
      errorMessage.value = errorData.detail;
      showSuccessAnimation.value = false;
    }
  } catch (error) {
    errorMessage.value = 'An error occurred while submitting the transaction. Please check your connection.';
    showSuccessAnimation.value = false;
  } finally {
    isSubmitting.value = false;
  }
};
</script>


<template>
  <div class="flex min-h-screen bg-gray-100">
    <!-- Sidebar -->
    <Sidebar :router="router" :user="storedUser" :role="role"/>

    <!-- Main Content -->
    <main class="flex-1 p-8 flex justify-center items-start overflow-y-auto">
      <div class="bg-white p-8 w-full h-full">
        <template v-if="!showSuccessAnimation">
          <h2 class="text-3xl font-bold text-gray-800 mb-8 text-center">Make models.com.stefvisser.springyield.Transaction</h2>
          <form @submit.prevent="handleTransactionSubmit">
            <!-- From models.com.stefvisser.springyield.Account Section -->
            <div class="mb-6">
              <label for="fromAccount" class="block text-sm font-medium text-gray-700 mb-1">From models.com.stefvisser.springyield.Account (IBAN)</label>
              <div class="relative flex items-stretch gap-4">
                <input
                    type="text"
                    id="fromAccount"
                    v-model.trim="transactionForm.fromAccount"
                    class="bg-white border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block w-full p-4 placeholder-gray-400 focus:z-10"
                    placeholder="NLXXBANK0123456789"
                    required
                />
                <button
                    type="button"
                    @click="toggleFromAddressBook"
                    title="Search From models.com.stefvisser.springyield.Account"
                    class="bg-gray-200 hover:bg-gray-300 text-gray-700 ml-2 px-4 py-4 rounded-lg  border border-l-0 border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 focus:z-10 flex items-center"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5"
                       stroke="currentColor" class="w-5 h-5">
                    <path stroke-linecap="round" stroke-linejoin="round"
                          d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z"/>
                  </svg>
                </button>
                <!-- From models.com.stefvisser.springyield.Account Search Popup -->
                <AddressBook
                    v-if="showFromAddressBook"
                    :userAccounts="[]"
                    :visible="showFromAddressBook"
                    @select-contact="selectFromAccount"
                    @close="showFromAddressBook = false"
                    class="address-book-popup absolute top-0 right-0 ml-2 z-20 bg-white p-4 rounded-lg shadow-xl border border-gray-300 w-96 max-h-[400px] overflow-y-auto"
                    style="min-height: 200px;"
                />
              </div>
              <p v-if="selectedFromAccountBalance !== null" class="text-xs text-gray-500 mt-1">
                Selected account balance: €{{ selectedFromAccountBalance?.toFixed(2) }}
              </p>
            </div>

            <!-- To models.com.stefvisser.springyield.Account Section -->
            <div class="mb-6">
              <label for="toAccount" class="block text-sm font-medium text-gray-700 mb-1">To models.com.stefvisser.springyield.Account (IBAN)</label>
              <div class="relative flex items-stretch gap-4">
                <input
                    type="text"
                    id="toAccount"
                    v-model.trim="transactionForm.toAccount"
                    class="bg-white border border-gray-300 text-gray-900 text-sm rounded-lg  focus:ring-blue-500 focus:border-blue-500 block w-full p-4 placeholder-gray-400 focus:z-10"
                    placeholder="NLXXBANK0123456789"
                    required
                />
                <button
                    type="button"
                    @click="toggleToAddressBook"
                    title="Search To models.com.stefvisser.springyield.Account"
                    class="bg-gray-200 hover:bg-gray-300 text-gray-700 ml-2 px-4 py-4 rounded-lg  border border-l-0 border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 focus:z-10 flex items-center"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5"
                       stroke="currentColor" class="w-5 h-5">
                    <path stroke-linecap="round" stroke-linejoin="round"
                          d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z"/>
                  </svg>
                </button>
                <!-- To models.com.stefvisser.springyield.Account Search Popup -->
                <AddressBook
                    v-if="showToAddressBook"
                    :userAccounts="[]"
                    :visible="showToAddressBook"
                    @select-contact="selectToAccount"
                    @close="showToAddressBook = false"
                    class="address-book-popup absolute top-0 right-0 ml-2 z-20 bg-white p-4 rounded-lg shadow-xl border border-gray-300 w-96 max-h-[400px] overflow-y-auto"
                    style="min-height: 200px;"
                />
              </div>
            </div>

            <!-- Amount Section -->
            <div class="mb-6">
              <label for="transferAmount" class="block text-sm font-medium text-gray-700 mb-1">Amount (€)</label>
              <input
                  type="number"
                  id="transferAmount"
                  v-model.number="transactionForm.transferAmount"
                  min="0.01"
                  step="0.01"
                  class="bg-white border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block w-full p-4 placeholder-gray-400"
                  placeholder="100.00"
                  required
              >
            </div>

            <!-- Description Section -->
            <div class="mb-8">
              <label for="description" class="block text-sm font-medium text-gray-700 mb-1">Description</label>
              <textarea
                  id="description"
                  v-model.trim="transactionForm.description"
                  rows="3"
                  class="bg-white border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block w-full p-4 placeholder-gray-400"
                  placeholder="Payment for goods/services"
              ></textarea>
            </div>

            <!-- Error Message -->
            <div v-if="errorMessage" class="mb-4 p-3 bg-red-100 text-red-700 rounded-lg border border-red-300">
              {{ errorMessage }}
            </div>

            <!-- Submit Button -->
            <div class="flex justify-center">
              <button
                  type="submit"
                  :disabled="isSubmitting"
                  class="bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white font-semibold py-3 px-6 rounded-lg shadow-md transition-colors duration-200"
              >
                <span v-if="isSubmitting">Processing...</span>
                <span v-else>Make models.com.stefvisser.springyield.Transaction</span>
              </button>
            </div>
          </form>
        </template>

        <!-- Success Animation -->
        <template v-else>
          <div class="flex flex-col items-center justify-center bg-white rounded-xl shadow-lg p-8">
            <div class="my-4 w-48 h-48 flex items-center justify-center text-gray-400">
              <div class="w-24 h-24 bg-green-500 rounded-full flex items-center justify-center">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2"
                     stroke="currentColor" class="w-12 h-12 text-white">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5"/>
                </svg>
              </div>
            </div>

            <div v-if="successMessage" class="mb-6 text-center">
              <p class="text-green-600 font-semibold text-xl">Success!</p>
              <p class="text-gray-800">{{ successMessage }}</p>
            </div>

            <div class="flex gap-4">
              <!-- New models.com.stefvisser.springyield.Transaction Button -->
              <button
                  @click="resetTransaction"
                  class="bg-green-600 hover:bg-green-700 text-white font-semibold py-3 px-8 rounded-lg shadow-md transition-colors duration-200"
              >
                Make New models.com.stefvisser.springyield.Transaction
              </button>

              <!-- Keep existing back to transactions button -->
              <button
                  @click="router.push('/admin/transactions')"
                  class="bg-blue-600 hover:bg-blue-700 text-white font-semibold py-3 px-8 rounded-lg shadow-md transition-colors duration-200"
              >
                Back to Transactions
              </button>
            </div>
          </div>
        </template>
      </div>
    </main>
  </div>
</template>


<!-- TODO: This is awful, just make it Tailwind-->
<style scoped>
/* Existing styles */
.min-h-\[500px\] {
  min-height: 500px;
}

/* Updated AddressBook styling overrides for light theme */
:deep(.address-book-popup) {
  background-color: white !important;
  border: 1px solid #e5e7eb !important;
}

/* Title styling */
:deep(.address-book-popup h3) {
  color: #111827 !important;
}

/* Search input styling */
:deep(.address-book-popup input[type="text"]) {
  background-color: #f9fafb !important;
  border: 1px solid #e5e7eb !important;
  color: #111827 !important;
}

/* Search results and contacts list */
:deep(.address-book-popup .max-h-48 > div) {
  border-bottom-color: #e5e7eb !important;
}

:deep(.address-book-popup .max-h-48 > div:hover) {
  background-color: #f3f4f6 !important;
}

/* Contact name and IBAN text */
:deep(.address-book-popup .font-medium) {
  color: #111827 !important;
}

:deep(.address-book-popup .text-neutral-400) {
  color: #6b7280 !important;
}

/* Save button */
:deep(.address-book-popup .text-sky-500) {
  color: #0ea5e9 !important;
}

:deep(.address-book-popup .text-sky-500:hover) {
  color: #0284c7 !important;
}

/* Form labels */
:deep(.address-book-popup label) {
  color: #374151 !important;
}

/* Input fields in add contact form */
:deep(.address-book-popup .bg-neutral-700) {
  background-color: #f9fafb !important;
  border-color: #e5e7eb !important;
  color: #111827 !important;
}

/* No results message */
:deep(.address-book-popup .text-neutral-400) {
  color: #6b7280 !important;
}

/* Section headers */
:deep(.address-book-popup h4) {
  color: #374151 !important;
}
</style>
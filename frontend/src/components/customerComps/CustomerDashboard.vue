<script setup>
import {ref, onMounted, computed} from 'vue';
import {useRouter} from 'vue-router';
import {getToken} from '@/utils/auth.js';
import AppHeader from "@/components/customerComps/AppHeader.vue";
import {formatCurrencyNoPlus} from "@/utils/formatters.js";
import apiFetch from '@/utils/api';

const props = defineProps({
  user: {
    type: Object,
    required: true
  }
});

const router = useRouter();
const allAccounts = ref([]);
const isLoadingAccounts = ref(true);

const groupedAccounts = computed(() => {
  const groups = new Map();
  allAccounts.value.forEach(account => {
    const type = account.accountType || 'UNKNOWN'; // Default type if undefined
    if (!groups.has(type)) {
      groups.set(type, []);
    }
    groups.get(type).push(account);
  });
  return groups;
});

const formatAccountType = (type) => {
  if (!type) return 'Unknown';
  return type.toLowerCase().replace('_', ' ');
};

const navigateToProfile = () => {
  router.push('/profile');
};

async function fetchAccounts() {
  isLoadingAccounts.value = true;

  try {
    const response = await apiFetch(`/account/${props.user.userId}`);
    if (response.ok) {
      const data = await response.json();
      // The API returns an object with an 'accounts' array
      allAccounts.value = data.accounts || [];
    } else {

      allAccounts.value = [];
    }
  } catch (error) {

    allAccounts.value = [];
  } finally {
    isLoadingAccounts.value = false;
  }
}

onMounted(async () => {
  const token = getToken();
  if (!token || !props.user || !props.user.userId) {
    // This case should ideally be handled by the parent Dashboard.vue redirecting to login
    isLoadingAccounts.value = false;
    return;
  }

  await fetchAccounts();
});

</script>

<template>
  <div
      class="customer-dashboard-app bg-gradient-to-b from-neutral-900 to-black min-h-screen flex flex-col text-neutral-300">
    <AppHeader :user="props.user" @navigateToProfile="navigateToProfile"/>

    <main class="flex-1 p-8 overflow-y-auto">
      <div v-if="isLoadingAccounts" class="text-center py-10">
        <p class="text-xl text-neutral-400">Loading accounts...</p>
        <!-- Optional: Add a spinner here -->
      </div>
      <div v-else-if="groupedAccounts.size === 0" class="text-center py-10">
        <p class="text-xl text-neutral-400">No accounts found.</p>
        <!-- Optional: Add a button to create an account -->
      </div>
      <div v-else>
        <div v-for="[type, accounts] in groupedAccounts" :key="type" class="mb-10">
          <h2 class="text-3xl font-bold text-neutral-100 mb-6 capitalize">{{ formatAccountType(type) }} Accounts</h2>
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <router-link v-for="a in accounts" :key="a.iban"
                         :to="{ name: 'AccountDetail', params: { iban: a.iban } }"
                         @click="router.dynamicPageData[`/account/${a.iban}`] = { accountType: a.accountType, iban: a.iban }"
                         class="bg-[#191B1D] p-6 rounded-[20px] shadow-lg hover:shadow-neutral-700/50 transition-shadow duration-300 cursor-pointer transform hover:scale-105">
              <div class="flex justify-between items-center mb-4">
                <h3 class="text-xl font-semibold text-neutral-100 truncate">{{ a.iban }}</h3>
                <span
                    :class="a.accountType === 'SAVINGS' ? 'bg-amber-500/20 text-amber-300' : 'bg-sy-400/20 text-sy-400'"
                    class="px-2 py-1 text-xs font-medium rounded-full">
                  {{ formatAccountType(a.accountType) }}
                </span>
              </div>
              <p class="text-2xl font-bold mb-1">Balance:</p>
              <p :class="a.balance < 0 ? 'text-red-400' : 'text-green-400 '" class="text-2xl font-bold mb-1">
                {{ formatCurrencyNoPlus(parseFloat(a.balance)) }}</p>
            </router-link>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
/* Styles for CustomerDashboard - account list */
.capitalize {
  text-transform: capitalize;
}
</style>


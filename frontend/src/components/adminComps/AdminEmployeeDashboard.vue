<template>
  <div class="flex min-h-screen bg-gradient-to-br from-sy-900 to-sy-700 text-white">
    <Sidebar :router="router" :user="user" :role="role" />
    <main class="flex-1 p-8 flex flex-col h-dvh overflow-y-auto">
      <h1 class="text-4xl font-bold mb-10 text-sy-100">Welcome, {{ user.firstName }}!</h1>

      <!-- Summary Cards -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-10">
        <div v-for="card in summaryCards" :key="card.title"
             class="bg-sy-800 p-6 rounded-xl shadow-lg hover:shadow-2xl transition-shadow duration-300 cursor-pointer hover:bg-sy-700 transform hover:-translate-y-1"
             @click="handleCardClick(card.navigation)">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg font-semibold text-sy-300">{{ card.title }}</h2>
            <img :src="card.icon" alt="" class="w-8 h-8 opacity-70" />
          </div>
          <p class="text-3xl font-bold text-sy-50">{{ card.value }}</p>
        </div>
      </div>

      <!-- Charts Section -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-10">
        <div class="bg-sy-800 p-6 rounded-xl shadow-lg min-h-[300px] lg:min-h-[400px] flex flex-col justify-center">
          <Bar v-if="transactionTypeData.labels.length" :data="transactionTypeData" :options="transactionChartOptions" />
          <p v-else class="text-sy-400 text-xl flex items-center justify-center h-full">Loading models.com.stefvisser.springyield.Transaction Data...</p>
        </div>
        <div class="bg-sy-800 p-6 rounded-xl shadow-lg min-h-[300px] lg:min-h-[400px] flex flex-col justify-center">
          <Doughnut v-if="accountTypeData.labels.length" :data="accountTypeData" :options="accountChartOptions" />
          <p v-else class="text-sy-400 text-xl flex items-center justify-center h-full">Loading models.com.stefvisser.springyield.Account Data...</p>
        </div>
      </div>

      <!-- Recent Activity -->
      <div class="bg-sy-800 p-6 rounded-xl shadow-lg">
        <h2 class="text-xl font-semibold text-sy-300 mb-4">Recent Activity</h2>
        <ul class="space-y-3">
          <li v-if="latestAccountCreationMessage" class="flex items-center justify-between p-3 bg-sy-700 rounded-lg hover:bg-sy-600 transition-colors duration-200">
            <p class="text-sy-200">{{ latestAccountCreationMessage }}</p>
            <span class="text-sm text-sy-400">Recently</span>
          </li>
          <li v-if="latestTransactionMessage" class="flex items-center justify-between p-3 bg-sy-700 rounded-lg hover:bg-sy-600 transition-colors duration-200">
            <p class="text-sy-200">{{ latestTransactionMessage }}</p>
            <span class="text-sm text-sy-400">Recently</span>
          </li>
          <li v-if="latestUnapprovedUserMessage" class="flex items-center justify-between p-3 bg-sy-700 rounded-lg hover:bg-sy-600 transition-colors duration-200">
            <p class="text-sy-200">{{ latestUnapprovedUserMessage }}</p>
            <span class="text-sm text-sy-400">Recently</span>
          </li>
          <li v-if="!latestAccountCreationMessage && !latestTransactionMessage && !latestUnapprovedUserMessage" class="text-sy-400 text-center p-3">
            No recent activity to display.
          </li>
        </ul>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { getToken } from '@/utils/auth.js';
import Sidebar from '@/components/adminComps/Sidebar.vue';
import { Bar, Doughnut } from 'vue-chartjs';
import { API_BASE_URL } from '@/config.js'; // <-- Import API_BASE_URL
import apiFetch from '@/utils/api';

const props = defineProps({
  user: {
    type: Object,
    required: true
  },
  role: {
    type: String,
    required: true
  }
});

const router = useRouter();

// Data specific to Admin/Employee Dashboard
const totalUsers = ref('0');
const totalAccounts = ref('0');
const totalTransactions = ref('0');
const pendingApprovals = ref('0');
const transactionTypeData = ref({ labels: [], datasets: [{ data: [], backgroundColor: [], label: 'Transactions by Type' }] });
const accountTypeData = ref({ labels: [], datasets: [{ data: [], backgroundColor: [], label: 'User models.com.stefvisser.springyield.Account Types' }] });
const latestAccountCreationMessage = ref('');
const latestTransactionMessage = ref('');
const latestUnapprovedUserMessage = ref('');

const chartOptions = ref({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { position: 'top', labels: { color: '#FFFFFF' } },
    title: { display: true, text: 'Chart Title', color: '#FFFFFF' }
  },
  scales: {
    y: { beginAtZero: true, ticks: { color: '#FFFFFF' }, grid: { color: 'rgba(255, 255, 255, 0.1)' } },
    x: { ticks: { color: '#FFFFFF' }, grid: { color: 'rgba(255, 255, 255, 0.1)' } }
  }
});

const transactionChartOptions = computed(() => ({
  ...chartOptions.value,
  plugins: { ...chartOptions.value.plugins, title: { ...chartOptions.value.plugins.title, text: 'Transactions by Type' } }
}));

const accountChartOptions = computed(() => ({
  ...chartOptions.value,
  plugins: { ...chartOptions.value.plugins, title: { ...chartOptions.value.plugins.title, text: 'User models.com.stefvisser.springyield.Account Types Distribution' } },
  scales: {}
}));

const summaryCards = computed(() => [
  { title: 'Total Users', value: totalUsers.value, icon: '/group_24dp_FFF.svg', navigation: { path: '/admin/users' } },
  { title: 'Total Accounts', value: totalAccounts.value, icon: '/credit_card_24dp_FFF.svg', navigation: { path: '/admin/accounts' } },
  { title: 'Total Transactions', value: totalTransactions.value, icon: '/currency_exchange_24dp_FFF.svg', navigation: { path: '/admin/transactions' } },
  { title: 'Pending Approvals', value: pendingApprovals.value, icon: '/more_horiz_FFF.svg', navigation: { path: '/admin/users', query: { roleFilter: 'UNAPPROVED' } } }
]);

const handleCardClick = (navigation) => {
  if (navigation) router.push(navigation);
};

onMounted(async () => {
  try {
    const allUsersResponse = await apiFetch(`/user/search?limit=1&offset=0`);
    if (allUsersResponse.ok) totalUsers.value = (await allUsersResponse.json()).totalCount.toString();
    else totalUsers.value = 'N/A';

    const pendingUsersResponse = await apiFetch(`/user/search?role=UNAPPROVED&limit=1&offset=0`);
    if (pendingUsersResponse.ok) pendingApprovals.value = (await pendingUsersResponse.json()).totalCount.toString();
    else pendingApprovals.value = 'N/A';

    const accountsResponse = await apiFetch(`/account/search?limit=1&offset=0`);
    if (accountsResponse.ok) totalAccounts.value = (await accountsResponse.json()).totalCount.toString();
    else totalAccounts.value = 'N/A';

    const transactionsResponse = await apiFetch(`/transactions/search?limit=1&offset=0`);
    if (transactionsResponse.ok) totalTransactions.value = (await transactionsResponse.json()).totalCount.toString();
    else totalTransactions.value = 'N/A';

    const allTransactionsResponse = await apiFetch(`/transactions/search?limit=1000&offset=0`);
    if (allTransactionsResponse.ok) {
      const allTransactionsData = await allTransactionsResponse.json();
      if (allTransactionsData.data) {
        const typeCounts = allTransactionsData.data.reduce((acc, curr) => {
          acc[curr.transactionType] = (acc[curr.transactionType] || 0) + 1; return acc;
        }, {});
        transactionTypeData.value = {
          labels: Object.keys(typeCounts),
          datasets: [{ data: Object.values(typeCounts), backgroundColor: ['#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0', '#9966FF'], label: 'Transactions by Type' }]
        };
      }
    }

    const allUsersForChartResponse = await apiFetch(`/user/search?limit=1000&offset=0`);
    if (allUsersForChartResponse.ok) {
      const allUsersDataForChart = await allUsersForChartResponse.json();
      if (allUsersDataForChart.data) {
        const roleCounts = allUsersDataForChart.data.reduce((acc, curr) => {
          acc[curr.role] = (acc[curr.role] || 0) + 1; return acc;
        }, {});
        accountTypeData.value = {
          labels: Object.keys(roleCounts),
          datasets: [{ data: Object.values(roleCounts), backgroundColor: ['#4BC0C0', '#FFCE56', '#FF6384', '#36A2EB', '#9966FF', '#FDB45C', '#949FB1'], label: 'User models.com.stefvisser.springyield.Account Types' }]
        };
      }
    }

    const latestAccountResponse = await apiFetch(`/account/search?limit=1&offset=0&sortBy=creationDate&sortDir=DESC`);
    if (latestAccountResponse.ok) {
      const d = await latestAccountResponse.json();
      if (d.data && d.data.length > 0) {
        const a = d.data[0];
        latestAccountCreationMessage.value = `models.com.stefvisser.springyield.Account ${a.iban} for ${a.user?.firstName || 'N/A'} ${a.user?.lastName || ''} created.`;
      }
    }

    const latestTransactionResponse = await apiFetch(`/transactions/search?limit=1&offset=0&sortBy=timestamp&sortDir=DESC`);
    if (latestTransactionResponse.ok) {
      const d = await latestTransactionResponse.json();
      if (d.data && d.data.length > 0) latestTransactionMessage.value = `Txn #${d.data[0].transactionId} (${d.data[0].transactionType}) for ${d.data[0].transferAmount} processed.`;
    }

    const latestUnapprovedUserResponse = await apiFetch(`/user/search?role=UNAPPROVED&limit=1&offset=0&sortBy=userId&sortDir=DESC`);
    if (latestUnapprovedUserResponse.ok) {
      const d = await latestUnapprovedUserResponse.json();
      if (d.data && d.data.length > 0) latestUnapprovedUserMessage.value = `User ${d.data[0].firstName} ${d.data[0].lastName} registered, needs approval.`;
    }

  } catch (error) {

    totalUsers.value = 'N/A'; pendingApprovals.value = 'N/A'; totalAccounts.value = 'N/A'; totalTransactions.value = 'N/A';
  }
});

</script>

<style scoped>
/* Styles specific to AdminEmployeeDashboard can be added here if needed */
</style>


<script setup>
import {computed, ref, onMounted} from 'vue';
import {useRouter} from 'vue-router';
import {getUserRole, getToken} from '@/utils/auth.js';
import AdminEmployeeDashboard from '@/components/adminComps/AdminEmployeeDashboard.vue';
import CustomerDashboard from '@/components/customerComps/CustomerDashboard.vue';
import {
  Chart as ChartJS,
  Title,
  Tooltip,
  Legend,
  BarElement,
  CategoryScale,
  LinearScale,
  ArcElement
} from 'chart.js';
import axios from "axios";
import {API_BASE_URL} from "@/config.js";

// Register Chart.js components globally for the application
ChartJS.register(Title, Tooltip, Legend, BarElement, CategoryScale, LinearScale, ArcElement);

const router = useRouter();
const isLoading = ref(true);
const storedUser = ref(null); // Initialize as null
const role = ref(null); // Initialize as null

// Role-based computed properties
const isAdminOrEmployee = computed(() => role.value === 'EMPLOYEE' || role.value === 'ADMIN');
const isCustomer = computed(() => role.value === 'CUSTOMER' || role.value === 'APPROVED'); // Assuming CUSTOMER is the primary role for the customer view

const logout = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
  // Reset local reactive state
  storedUser.value = null;
  role.value = null;
  router.push('/login');
};

onMounted(async () => {
  isLoading.value = true;
  const token = getToken();
  const userFromStorage = localStorage.getItem('user');

  if (!token || !userFromStorage) {
    router.push('/login');
    isLoading.value = false;
    return;
  }

  try {
    storedUser.value = JSON.parse(userFromStorage);
    role.value = getUserRole(); // This function already handles token decoding and errors
  } catch (error) {

    logout(); // Log out if user data is corrupted or role cannot be determined
    isLoading.value = false;
    return;
  }

  // No data fetching here, that's handled by child components
  isLoading.value = false;
});

const approveMyselfDemo = async () => {
  // This function is not used in the current implementation
  // It can be removed or implemented as needed
  console.log('Approve Myself Demo function called');

  try {
    await axios.put(
        `${API_BASE_URL}/user/${props.user.userId}/approve`,
        {
          dailyLimit: 1000,
          absoluteLimit: 5000
        },
        {
          headers: {
            Authorization: `Bearer ${props.token}`
          }
        }
    )
    emit('approve-success', props.user)
  } catch (err) {
    emit('approve-error', err.response?.data?.message || 'Failed to approve user.')
  }
};
</script>


<template>
  <!-- Loading State -->
  <div v-if="isLoading"
       class="fixed inset-0 z-[100] flex flex-col items-center justify-center bg-slate-900 text-white p-8 text-center">
    <h1 class="text-3xl font-bold mb-4">Loading Dashboard...</h1>
    <p class="text-xl">Please wait while we prepare your experience.</p>
  </div>

  <!-- Main Content (conditionally rendered after loading) -->
  <div v-else>
    <!-- Unapproved user notification -->
    <div v-if="role === 'UNAPPROVED'"
         class="fixed inset-0 z-50 flex flex-col items-center justify-center bg-sy-700 text-white p-8 text-center">
      <h1 class="text-4xl font-bold mb-4">Account Pending Approval</h1>
      <p class="text-xl mb-6">Your account is currently awaiting approval from an administrator.</p>
      <p class="text-lg">Please check back later or contact support if you have any questions.</p>
      <button @click="logout"
              class="mt-8 bg-white hover:bg-gray-200 text-sy-700 font-bold py-2 px-6 rounded-lg shadow-md cursor-pointer">
        Logout
      </button>
<!--      <button @click="approveMyselfDemo"-->
<!--              class="mt-8 bg-white hover:bg-gray-200 text-sy-700 font-bold py-2 px-6 rounded-lg shadow-md cursor-pointer">-->
<!--        Approve Myself-->
<!--      </button>-->
    </div>


    <!-- Admin/Employee Dashboard -->
    <AdminEmployeeDashboard v-else-if="isAdminOrEmployee" :user="storedUser" :role="role"/>

    <!-- Customer Banking App View -->
    <CustomerDashboard v-else-if="isCustomer" :user="storedUser"/>

    <!-- Fallback for any other role or if no role is defined -->
    <div v-else
         class="fixed inset-0 z-50 flex flex-col items-center justify-center bg-gray-800 text-white p-8 text-center">
      <h1 class="text-4xl font-bold mb-4">Access Denied</h1>
      <p class="text-xl">You do not have permission to view this page or your role is not recognized.</p>
      <button @click="logout"
              class="mt-8 bg-sky-500 hover:bg-sky-600 text-white font-bold py-2 px-6 rounded-lg shadow-md">
        Logout
      </button>
    </div>
  </div>
</template>

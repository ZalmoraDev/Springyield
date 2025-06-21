<script setup>
import {ref, onMounted} from 'vue';
import {useRouter} from 'vue-router';
import {getToken} from '@/utils/auth.js';

const user = ref(null);
const isLoadingUser = ref(false);
const router = useRouter();

const fetchCurrentUser = () => {
  const token = getToken();
  const userFromStorage = localStorage.getItem('user');

  if (token && userFromStorage) {
    try {
      user.value = JSON.parse(userFromStorage);
    } catch (e) {

      // Clear corrupted data and potentially logout/redirect
      localStorage.removeItem('user');
      localStorage.removeItem('token');
      user.value = null;
      router.push('/login');
    }
  } else {
    user.value = null; // Ensure user is null if no token/storage
  }
  isLoadingUser.value = false;
};

onMounted(() => {
  fetchCurrentUser();
  // Optional: Listen for storage changes or custom events if user data can change elsewhere
  window.addEventListener('storage', (event) => {
    if (event.key === 'user' || event.key === 'token') {
      fetchCurrentUser();
    }
  });
});

// Function to update user (e.g., after login or profile update)
// This could be called from Login.vue or Profile.vue via events or a shared service
const updateUser = (newUser) => {
  user.value = newUser;
  if (newUser) {
    localStorage.setItem('user', JSON.stringify(newUser));
  } else {
    localStorage.removeItem('user');
  }
};
</script>


<template>
  <main>
    <router-view v-if="!isLoadingUser" :user="user" @user-updated="updateUser"></router-view>
    <div v-else
         class="fixed inset-0 z-[100] flex flex-col items-center justify-center bg-slate-900 text-white p-8 text-center">
      <h1 class="text-3xl font-bold mb-4">Loading Application...</h1>
    </div>
  </main>
</template>

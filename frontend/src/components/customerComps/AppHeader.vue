<script setup>
import { useRouter } from 'vue-router';

const props = defineProps({
  user: {
    type: Object,
    default: null
  },
  showProfileButton: {
    type: Boolean,
    default: true
  },
  logoRedirectPath: {
    type: String,
    default: '/'
  }
});

const router = useRouter();

const navigateToProfile = (event) => {
  event.preventDefault(); // Prevent default button behavior
  event.stopPropagation(); // Stop event bubbling
  router.push('/profile');
};

const handleLogoClick = () => {
  router.push(props.logoRedirectPath);
};

const logout = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
  router.push('/login');
};
</script>

<template>
  <header class="bg-[#191B1D] shadow-xl p-5 flex justify-between items-center sticky top-0 z-40">
    <div class="flex items-center">
      <div class="cursor-pointer" @click.stop="handleLogoClick">
        <img src="/logo/logo-wt.svg" alt="Springyield Logo" class="h-10 mr-3">
      </div>

    </div>

    <div class="flex items-center">
      <span class="text-neutral-400 mr-4 text-lg">Welcome, {{ user ? user.firstName : 'models.com.stefvisser.springyield.User' }}!</span>

      <button
          type="button"
          @click.stop="navigateToProfile"
          class="mr-4 bg-neutral-800 hover:bg-neutral-700 text-white font-semibold py-2 px-5 rounded-lg shadow-md transition-colors duration-200 cursor-pointer"
      >
        models.com.stefvisser.springyield.User Profile
      </button>

      <button
          type="button"
          @click.stop="logout"
          class="bg-neutral-800 hover:bg-neutral-700 text-white font-semibold py-2 px-5 rounded-lg shadow-md transition-colors duration-200 cursor-pointer"
      >
        Logout
      </button>


    </div>
  </header>
</template>
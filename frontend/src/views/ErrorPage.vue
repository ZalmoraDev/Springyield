<script setup>
import { useRouter, useRoute } from 'vue-router';
import { ref, onMounted } from 'vue';

const router = useRouter();
const route = useRoute();

// Default error state
const errorState = ref({
  code: '403',
  title: 'Forbidden',
  message: 'You don\'t have permission to access this resource.'
});

// Set error based on route params
onMounted(() => {
  // Get error type from route params or query
  const errorType = route.params.errorType || route.query.type || '403';

  // Valid error types
  const validErrorTypes = ['401', '403', '404'];

  // Check if errorType is valid, redirect to 404 if not
  if (!validErrorTypes.includes(errorType)) {
    router.replace('/error/404');
    return;
  }

  switch (errorType) {
    case '404':
      errorState.value = {
        code: '404',
        title: 'Page Not Found',
        message: 'Page not found. The page you are looking for does not exist.'
      };
      break;
    case '401':
      errorState.value = {
        code: '401',
        title: 'Unauthorized',
        message: 'You don\'t have permission to access this resource.'
      };
      break;
    case '403':
    default:
      // Default is already set
      break;
  }
});

const goBack = () => {
  router.go(-1);
};

const goHome = () => {
  router.push('/');
};
</script>


<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-100">
    <div class="bg-white p-8 rounded-2xl shadow-xl max-w-md w-full text-center">
      <img src="/logo/logo-bt.svg" alt="Logo" class="w-24 h-24 mx-auto mb-4">

      <div class="bg-red-100 text-red-700 p-4 rounded-xl mb-6">
        <h1 class="text-3xl font-bold mb-2">{{ errorState.code }}</h1>
        <h2 class="text-xl font-semibold mb-4">{{ errorState.title }}</h2>
        <p class="mb-4">{{ errorState.message }}</p>
      </div>

      <div class="space-y-4">
        <p>Neem contact op met de beheerder als je denkt dat dit een fout is.</p>
        <div class="flex justify-center space-x-4">
          <button
              @click="goBack"
              class="px-4 py-2 bg-gray-200 text-gray-700 rounded-xl hover:bg-gray-300 transition"
          >
            Terug
          </button>
          <button
              @click="goHome"
              class="px-4 py-2 bg-sy-600 text-white rounded-xl hover:bg-sy-700 transition"
          >
            Naar Dashboard
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

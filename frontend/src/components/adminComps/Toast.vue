<script setup>
defineProps({
  message: String,
  show: Boolean
})
</script>

<template>
  <Transition
      enter-active-class="transform ease-out duration-300 transition"
      enter-from-class="translate-y-2 opacity-0 sm:translate-y-0 sm:translate-x-2"
      enter-to-class="translate-y-0 opacity-100 sm:translate-x-0"
      leave-active-class="transition ease-in duration-100"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
  >
    <div v-if="show" class="fixed top-4 right-4 z-50">
      <div class="bg-green-500 text-white px-4 py-2 rounded-lg shadow-lg">
        {{ message }}
      </div>
    </div>
  </Transition>
</template>
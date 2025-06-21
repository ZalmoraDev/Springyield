<template>
  <div class="bg-white rounded-lg shadow p-6 mb-6">
    <div v-if="title" class="flex justify-between items-center mb-4">
      <h2 class="text-lg font-semibold text-gray-700">{{ title }}</h2>
      <slot name="header-extra"></slot>
    </div>
    <slot></slot>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: ''
  }
})
</script>


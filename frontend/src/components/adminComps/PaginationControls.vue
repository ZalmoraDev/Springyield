<template>
  <div class="border-t border-gray-200 px-6 py-4 flex items-center justify-between">
    <div class="flex-1 flex justify-between items-center">
      <div>
        <p v-if="totalItems > 0" class="text-sm text-gray-700">
          Showing
          <span class="font-medium">{{ (currentPage * itemsPerPage) + 1 }}</span>
          to
          <span class="font-medium">{{ Math.min((currentPage + 1) * itemsPerPage, totalItems) }}</span>
          of
          <span class="font-medium">{{ totalItems }}</span>
          results
        </p>
        <p v-else class="text-sm text-gray-700">No results</p>
      </div>
      <div class="flex space-x-2">
        <button
          @click="$emit('previous-page')"
          :disabled="currentPage === 0"
          :class="{
            'opacity-50 cursor-not-allowed': currentPage === 0,
            'cursor-pointer hover:bg-gray-50': currentPage !== 0
          }"
          class="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white transition-colors"
        >
          Previous
        </button>
        <button
          @click="$emit('next-page')"
          :disabled="(currentPage + 1) * itemsPerPage >= totalItems && totalItems > 0"
          :class="{
            'opacity-50 cursor-not-allowed': (currentPage + 1) * itemsPerPage >= totalItems && totalItems > 0,
            'cursor-pointer hover:bg-gray-50': !((currentPage + 1) * itemsPerPage >= totalItems && totalItems > 0)
          }"
          class="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white transition-colors"
        >
          Next
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  currentPage: {
    type: Number,
    required: true
  },
  itemsPerPage: {
    type: Number,
    required: true
  },
  totalItems: {
    type: Number,
    required: true
  }
})
defineEmits(['previous-page', 'next-page'])
</script>


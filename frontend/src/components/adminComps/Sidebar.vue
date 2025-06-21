<script setup lang="ts">
import type {Router} from 'vue-router'
import {defineProps, ref, watch, computed} from 'vue' // Added computed
import { formatNameWithInitial } from '@/utils/formatters.js'

const props = defineProps<{
  router: Router,
  user: {
    firstName: string,
    lastName: string,
    userId?: number // Add userId to the type definition
  },
  role: string
}>()

const isSidebarManuallyOpened = ref(false) // New reactive state

// Watch for route changes to open the sidebar
watch(() => props.router.currentRoute.value.path, () => {
  isSidebarManuallyOpened.value = true
}, {immediate: true}) // immediate: true to also open on initial load

// Get the user ID from the stored user object
const userId = computed(() => props.user?.userId || JSON.parse(localStorage.getItem('user'))?.userId)

const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  props.router.push('/login')
}

const isActive = (path: string) => {
  return props.router.currentRoute.value.path === path
}

// Check if current route is the user details page for the logged in user
const isUserProfileActive = () => {
  const currentPath = props.router.currentRoute.value.path
  return currentPath === `/admin/user/${userId.value}`
}
</script>

<template>
  <aside
      :class="[
      'group bg-sy-600 text-white flex flex-col transition-[width] duration-300 overflow-hidden min-h-screen h-full sticky top-0 hover:w-40',
      { 'w-40': isSidebarManuallyOpened, 'w-16': !isSidebarManuallyOpened }
    ]"
      @mouseleave="isSidebarManuallyOpened = false"
  >
    <nav class="flex-1 p-2">

      <div class="flex mb-2 items-center gap-3 p-3 rounded-lg transition-all">
        <img src="/logo/logo-w.svg" alt="Profile Icon" class="w-5 h-5"/>
        <span
            :class="['font-bold whitespace-nowrap transition-opacity', isSidebarManuallyOpened ? 'opacity-100' : 'opacity-0 group-hover:opacity-100']">Springyield</span>
      </div>
      <div class="p-2 border-t flex flex-col gap-2 border-sy-800"></div>
      <ul class="space-y-2">
        <li>
          <router-link to="/"
                       :class="['flex items-center gap-3 p-3 rounded-lg transition-all', isActive('/') ? 'bg-sy-800' : 'hover:bg-sy-700']">
            <img src="/home_24dp_FFF.svg" alt="Home Icon" class="w-5 h-5"/>
            <span
                :class="['whitespace-nowrap transition-opacity', isSidebarManuallyOpened ? 'opacity-100' : 'opacity-0 group-hover:opacity-100']">Dashboard</span>
          </router-link>
        </li>
        <li v-if="props.role === 'EMPLOYEE' || props.role === 'ADMIN'">
          <router-link to="/admin/users"
                       :class="['flex items-center gap-3 p-3 rounded-lg transition-all', isActive('/admin/users') ? 'bg-sy-800' : 'hover:bg-sy-700']">
            <img src="/group_24dp_FFF.svg" alt="Users Icon" class="w-5 h-5"/>
            <span
                :class="['whitespace-nowrap transition-opacity', isSidebarManuallyOpened ? 'opacity-100' : 'opacity-0 group-hover:opacity-100']">Users</span>
          </router-link>
        </li>
        <li>
          <router-link to="/admin/accounts"
                       :class="['flex items-center gap-3 p-3 rounded-lg transition-all', isActive('/admin/accounts') ? 'bg-sy-800' : 'hover:bg-sy-700']">
            <img src="/credit_card_24dp_FFF.svg" alt="Accounts Icon" class="w-5 h-5"/>
            <span
                :class="['whitespace-nowrap transition-opacity', isSidebarManuallyOpened ? 'opacity-100' : 'opacity-0 group-hover:opacity-100']">Accounts</span>
          </router-link>
        </li>
        <li>
          <router-link to="/admin/transactions"
                       :class="['flex items-center gap-3 p-3 rounded-lg transition-all', isActive('/admin/transactions') ? 'bg-sy-800' : 'hover:bg-sy-700']">
            <img src="/currency_exchange_24dp_FFF.svg" alt="models.com.stefvisser.springyield.Transaction Icon" class="w-5 h-5"/>
            <span
                :class="['whitespace-nowrap transition-opacity', isSidebarManuallyOpened ? 'opacity-100' : 'opacity-0 group-hover:opacity-100']">Transactions</span>
          </router-link>
        </li>
      </ul>
    </nav>

    <div class="p-2 flex flex-col gap-2 border-sy-800">
      <router-link
        :to="{ name: 'UserDetails', params: { userId: userId } }"
        :class="['flex items-center gap-3 p-3 rounded-lg transition-all', isUserProfileActive() ? 'bg-sy-800' : 'hover:bg-sy-700']">
        <img src="/account_circle_24dp_FFF.svg" alt="Profile Icon" class="w-5 h-5"/>
        <span
            :class="['whitespace-nowrap truncate transition-opacity', isSidebarManuallyOpened ? 'opacity-100' : 'opacity-0 group-hover:opacity-100']">
          {{ formatNameWithInitial(props.user.firstName, props.user.lastName) }}
        </span>
      </router-link>

      <button @click="logout"
              class="w-full flex items-center gap-3 p-3 rounded-lg bg-red-600 hover:bg-red-700 transition-all cursor-pointer">
        <img src="/logout_24dp_FFF.svg" alt="Logout Icon" class="w-5 h-5 flex-shrink-0"/>
        <span
            :class="['whitespace-nowrap transition-opacity duration-300', isSidebarManuallyOpened ? 'opacity-100' : 'opacity-0 group-hover:opacity-100']">Uitloggen</span>
      </button>
    </div>
  </aside>
</template>


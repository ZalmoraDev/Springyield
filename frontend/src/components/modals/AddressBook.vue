<script setup>
import {ref, computed, onMounted, watch} from 'vue';

const props = defineProps({
  userAccounts: {
    type: Array,
    default: () => []
  },
  visible: {
    type: Boolean,
    default: false
  },
  position: {
    type: Object,
    default: () => ({top: 0, right: 0})
  }
});

const emit = defineEmits(['select-contact', 'close']);

const contacts = ref([]);
const searchTerm = ref('');
const searchResults = ref([]);
const isSearching = ref(false);
const searchTimeout = ref(null);

// Load contacts from local storage on mount
onMounted(() => {
  const savedContacts = localStorage.getItem('addressBook');
  if (savedContacts) {
    contacts.value = JSON.parse(savedContacts);
  }

  // Add user's own accounts to the address book
  const ownAccounts = props.userAccounts.map(account => {
    let type = account.accountType;
    return {
      firstName: 'My Account',
      lastName: '(' + type + ')',
      iban: account.iban,
      isOwn: true
    };
  });

  // Make sure we don't add duplicates
  ownAccounts.forEach(ownAccount => {
      contacts.value.push(ownAccount);
  });
});

// Filtered contacts based on local search term
const filteredContacts = computed(() => {
  if (!searchTerm.value) return contacts.value;

  const term = searchTerm.value.toLowerCase();
  return contacts.value.filter(contact =>
      contact.firstName.toLowerCase().includes(term) ||
      contact.lastName.toLowerCase().includes(term) ||
      contact.iban.toLowerCase().includes(term)
  );
});

// Select a contact to use in transaction
const selectContact = (contact) => {
  emit('select-contact', contact);
  emit('close');
};

// Close when clicked outside
const onClickOutside = () => {
  emit('close');
};
</script>

<script>
// Click outside directive
export default {
  directives: {
    clickOutside: {
      mounted(el, binding) {
        el.clickOutsideEvent = function (event) {
          // Check if the click is outside the element
          if (!(el === event.target || el.contains(event.target))) {
            // Ensure the element is still part of the document (e.g., not removed by v-if)
            if (document.body.contains(el)) {
              binding.value(event); // Call the handler (onClickOutside in script setup)
            }
          }
        };
        // Delay adding the event listener to allow the current event cycle to complete.
        // This prevents the click that opens the popup from immediately closing it.
        el.__clickOutsideTimeout__ = setTimeout(() => {
          // Add listener in the capture phase to catch clicks early.
          document.addEventListener('click', el.clickOutsideEvent, true);
        }, 0);
      },
      unmounted(el) {
        // Clear the timeout and remove the event listener
        clearTimeout(el.__clickOutsideTimeout__);
        document.removeEventListener('click', el.clickOutsideEvent, true);
      }
    }
  }
}
</script>


<template>
  <div
      class="address-book-popup bg-neutral-800 border border-neutral-600 rounded-lg p-4 shadow-lg"
      :class="{ 'visible': visible, 'hidden': !visible }"
      :style="{
      position: 'absolute',
      right: position.right + 'px',
      top: position.top + 'px',
      width: '320px',
      zIndex: 50
    }"
      v-click-outside="onClickOutside"
  >
    <div class="flex justify-between items-center mb-4">
      <h3 class="text-lg font-semibold text-neutral-200">My Accounts</h3>
    </div>

    <!-- Own Accounts list -->
    <div v-if="filteredContacts.length > 0">
      <div class="max-h-48 overflow-y-auto">
        <div
            v-for="(contact, index) in filteredContacts"
            :key="index"
            class="p-2 border-b border-neutral-700 hover:bg-neutral-700 cursor-pointer flex justify-between items-center group"
            @click="selectContact(contact)"
        >
          <div>
            <div class="font-medium text-neutral-200">{{ contact.firstName }} {{ contact.lastName }}</div>
            <div class="text-xs text-neutral-400">{{ contact.iban }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

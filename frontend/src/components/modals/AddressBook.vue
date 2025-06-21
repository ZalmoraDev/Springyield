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
      <h3 class="text-lg font-semibold text-neutral-200">Address Book</h3>
      <button @click="showAddForm = !showAddForm" class="text-sky-500 hover:text-sky-400 text-sm">
        {{ showAddForm ? 'Cancel' : '+ Add Contact' }}
      </button>
    </div>

    <!-- Add new contact form -->
    <div v-if="showAddForm" class="mb-4 p-3 border border-neutral-700 bg-neutral-900 rounded-lg">
      <div class="grid grid-cols-1 gap-3">
        <div>
          <label for="contactFirstName" class="block text-xs font-medium text-neutral-400 mb-1">First Name</label>
          <input
            type="text"
            id="contactFirstName"
            v-model.trim="newContact.firstName"
            class="bg-neutral-700 border border-neutral-600 text-neutral-200 text-sm rounded-lg focus:ring-sky-500 focus:border-sky-500 block w-full p-2 placeholder-neutral-400"
            placeholder="John"
            required
          />
        </div>
        <div>
          <label for="contactLastName" class="block text-xs font-medium text-neutral-400 mb-1">Last Name</label>
          <input
            type="text"
            id="contactLastName"
            v-model.trim="newContact.lastName"
            class="bg-neutral-700 border border-neutral-600 text-neutral-200 text-sm rounded-lg focus:ring-sky-500 focus:border-sky-500 block w-full p-2 placeholder-neutral-400"
            placeholder="Doe"
            required
          />
        </div>
        <div>
          <label for="contactIban" class="block text-xs font-medium text-neutral-400 mb-1">IBAN</label>
          <input
            type="text"
            id="contactIban"
            v-model.trim="newContact.iban"
            class="bg-neutral-700 border border-neutral-600 text-neutral-200 text-sm rounded-lg focus:ring-sky-500 focus:border-sky-500 block w-full p-2 placeholder-neutral-400"
            placeholder="NLXXBANK0123456789"
            required
          />
        </div>
      </div>
    </div>

    <!-- Search box -->
    <div class="mb-4">
      <div class="relative">
        <input
          type="text"
          v-model="searchTerm"
          placeholder="Search contacts..."
          class="bg-neutral-700 border border-neutral-600 text-neutral-200 text-sm rounded-lg focus:ring-sky-500 focus:border-sky-500 block w-full p-2.5 placeholder-neutral-400"
        />
        <div v-if="isSearching" class="absolute right-2 top-1/2 transform -translate-y-1/2">
          <div class="animate-spin h-4 w-4 border-t-2 border-sky-500 rounded-full"></div>
        </div>
      </div>
      <p v-if="searchTerm && searchTerm.length > 0 && searchTerm.length < 3" class="text-xs text-neutral-400 mt-1">
        Type at least 3 characters to search online
      </p>
    </div>

    <!-- Search results section -->
    <div v-if="searchResults.length > 0" class="mb-4 border-t border-neutral-700 pt-3">
      <h4 class="text-sm font-medium text-neutral-300 mb-2">Search Results</h4>
      <div class="max-h-48 overflow-y-auto">
        <div
          v-for="(contact, index) in searchResults"
          :key="`search-${index}`"
          class="p-2 border-b border-neutral-700 hover:bg-neutral-700 cursor-pointer flex justify-between items-center group"
          @click="selectContact(contact)"
        >
          <div>
            <div class="font-medium text-neutral-200">{{ contact.firstName }} {{ contact.lastName }}</div>
            <div class="text-xs text-neutral-400">{{ contact.iban }}</div>
          </div>
          <button
            @click.stop="addToSavedContacts(contact)"
            class="text-sky-500 hover:text-sky-400 opacity-0 group-hover:opacity-100 transition-opacity text-sm"
          >
            Save
          </button>
        </div>
      </div>
    </div>

    <!-- Saved contacts list -->
    <div v-if="filteredContacts.length > 0">
      <h4 class="text-sm font-medium text-neutral-300 mb-2">Saved Contacts</h4>
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

    <div v-if="filteredContacts.length === 0 && searchResults.length === 0" class="text-center py-4 text-neutral-400 text-sm">
      {{ searchTerm ? 'No matching contacts found' : 'No saved contacts yet' }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import apiFetch from '@/utils/api.js';

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
    default: () => ({ top: 0, right: 0 })
  }
});

const emit = defineEmits(['select-contact', 'close']);

const contacts = ref([]);
const showAddForm = ref(false);
const searchTerm = ref('');
const newContact = ref({
  firstName: '',
  lastName: '',
  iban: ''
});
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
    if (!contacts.value.some(contact => contact.iban === ownAccount.iban)) {
      contacts.value.push(ownAccount);
    }
  });
});

// Save contacts to local storage whenever they change
watch(contacts, (newContacts) => {
  // Only save non-own accounts
  const contactsToSave = newContacts.filter(contact => !contact.isOwn);
  localStorage.setItem('addressBook', JSON.stringify(contactsToSave));
}, { deep: true });

// Watch for changes in search term and trigger API search
watch(searchTerm, (newSearchTerm) => {
  // Clear previous timeout if it exists
  if (searchTimeout.value) {
    clearTimeout(searchTimeout.value);
  }

  // Clear previous search results
  searchResults.value = [];

  // Only search if term is at least 3 characters
  if (newSearchTerm.length >= 3) {
    isSearching.value = true;

    // Add a small delay to avoid making API calls for each keystroke
    searchTimeout.value = setTimeout(() => {
      searchAddressBook();
    }, 500);
  }
});

// Search for contacts using the backend API
const searchAddressBook = async () => {
  if (searchTerm.value.length < 3) {
    isSearching.value = false;
    return;
  }

  try {
    // Determine if search term matches a first name or last name pattern
    const searchTerms = searchTerm.value.trim().split(' ').filter(term => term.length > 0);

    // Construct URL with query parameters
    let url = '/account/addressbook/search?';
    if (searchTerms) {
      url += `query=${encodeURIComponent(searchTerms)}`;
    }

    const response = await apiFetch(url);

    if (response.ok) {
      const data = await response.json();

      // Filter out entries that are already in the contacts list
      searchResults.value = data.filter(result =>
        !contacts.value.some(contact => contact.iban === result.iban)
      );
    }
  } finally {
    isSearching.value = false;
  }
};

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

// Validate new contact form
const isNewContactValid = computed(() => {
  return (
    newContact.value.firstName.trim() !== '' &&
    newContact.value.lastName.trim() !== '' &&
    newContact.value.iban.trim() !== ''
  );
});

// Save a new contact
const saveContact = () => {
  if (!isNewContactValid.value) return;

  // Check for duplicates
  if (contacts.value.some(c => c.iban === newContact.value.iban)) {
    alert('This IBAN already exists in your address book');
    return;
  }

  contacts.value.push({...newContact.value});

  // Reset form
  newContact.value = {
    firstName: '',
    lastName: '',
    iban: ''
  };
  showAddForm.value = false;
};

// Add a search result to saved contacts
const addToSavedContacts = (contact) => {
  if (!contacts.value.some(c => c.iban === contact.iban)) {
    contacts.value.push({...contact});
  }
};

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
        el.clickOutsideEvent = function(event) {
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

<style scoped>
.address-book-popup {
  max-height: 500px;
  overflow-y: auto;
}
</style>


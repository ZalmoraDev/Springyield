import { ref, watch, computed } from 'vue'
import axios from 'axios'

/**
 * A composable function to handle paginated data fetching, filtering, and searching.
 *
 * @param {object} config - Configuration object for the composable.
 * @param {string} config.apiUrl - The base URL for the API endpoint.
 * @param {number} [config.initialItemsPerPage=25] - The initial number of items to display per page.
 * @param {object} [config.initialFilters={}] - An object containing initial filter values.
 * @param {string} [config.searchQueryParamName='query'] - The URL parameter name for the search query.
 * @param {object} [config.filterParamNames={}] - An object mapping internal filter keys to URL parameter names (e.g., { roleFilter: 'role' }).
 * @returns {object} An object containing reactive state and functions to manage paginated data.
 */
export function usePaginatedData(config) {
  const {
    apiUrl,
    initialItemsPerPage = 25,
    initialFilters = {},
    searchQueryParamName = 'query',
    filterParamNames = {} // e.g., { roleFilter: 'role', typeFilter: 'type' }
  } = config

  const items = ref([])
  const loading = ref(false)
  const error = ref(null)
  const searchQuery = ref('')
  const filters = ref({ ...initialFilters })
  const debounceTimeout = ref(null)
  const currentPage = ref(0)
  const totalItems = ref(0)
  const itemsPerPage = ref(initialItemsPerPage)

  /**
   * Fetches data from the API based on the current search query, filters, and pagination state.
   *
   * @param {string} [query=searchQuery.value] - The search query string.
   * @param {number} [page=currentPage.value] - The page number to fetch (0-indexed).
   * @returns {Promise<void>} A promise that resolves when the data has been fetched.
   */
  const fetchData = async (query = searchQuery.value, page = currentPage.value) => {
    loading.value = true
    error.value = null
    try {
      const params = new URLSearchParams({
        limit: itemsPerPage.value.toString(),
        offset: (page * itemsPerPage.value).toString()
      })

      if (query.trim()) {
        params.append(searchQueryParamName, query.trim())
      }

      for (const filterKey in filters.value) {
        const paramName = filterParamNames[filterKey] || filterKey
        const filterValue = filters.value[filterKey]
        if (filterValue && filterValue !== `All ${paramName.charAt(0).toUpperCase() + paramName.slice(1)}s` && filterValue !== 'All Types' && filterValue !== 'All Accounts' && filterValue !== 'All Users') {
          params.append(paramName, String(filterValue).toUpperCase())
        }
      }

      const response = await axios.get(apiUrl, {
        params,
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      })
      items.value = response.data.data
      totalItems.value = response.data.totalCount
    } catch (err) {
      items.value = []
      totalItems.value = 0
    } finally {
      loading.value = false
    }
  }

  // Watch for changes in the search query and fetch data with debounce
  watch(searchQuery, (newQuery) => {
    if (debounceTimeout.value) {
      clearTimeout(debounceTimeout.value)
    }
    debounceTimeout.value = setTimeout(() => {
      currentPage.value = 0
      fetchData(newQuery)
    }, 300)
  })

  // Watch for changes in filters and fetch data
  watch(filters, () => {
    currentPage.value = 0
    fetchData()
  }, { deep: true })

  /**
   * Navigates to the previous page of data and fetches it.
   */
  const goToPreviousPage = () => {
    if (currentPage.value > 0) {
      currentPage.value -= 1
      fetchData(searchQuery.value, currentPage.value)
    }
  }

  /**
   * Navigates to the next page of data and fetches it.
   */
  const goToNextPage = () => {
    if ((currentPage.value + 1) * itemsPerPage.value < totalItems.value) {
      currentPage.value += 1
      fetchData(searchQuery.value, currentPage.value)
    }
  }

  /**
   * Computed property that calculates the total number of pages.
   * @returns {number} The total number of pages.
   */
  const pageCount = computed(() => {
    if (itemsPerPage.value === 0) return 0;
    return Math.ceil(totalItems.value / itemsPerPage.value);
  });

  /**
   * @typedef {object} PaginatedDataReturn
   * @property {import('vue').Ref<Array<object>>} items - The array of fetched items for the current page.
   * @property {import('vue').Ref<boolean>} loading - A boolean indicating if data is currently being fetched.
   * @property {import('vue').Ref<Error|null>} error - An error object if an error occurred during fetching, otherwise null.
   * @property {import('vue').Ref<string>} searchQuery - The current search query string.
   * @property {import('vue').Ref<object>} filters - An object containing the current filter values.
   * @property {import('vue').Ref<number>} currentPage - The current page number (0-indexed).
   * @property {import('vue').Ref<number>} totalItems - The total number of items available from the API for the current query/filters.
   * @property {import('vue').Ref<number>} itemsPerPage - The number of items to display per page.
   * @property {function(string=, number=): Promise<void>} fetchData - Function to fetch data from the API.
   * @property {function(): void} goToPreviousPage - Function to navigate to the previous page.
   * @property {function(): void} goToNextPage - Function to navigate to the next page.
   * @property {import('vue').ComputedRef<number>} pageCount - The total number of pages.
   */

  /**
   * @returns {PaginatedDataReturn} The paginated data management object.
   */
  return {
    items,
    loading,
    error,
    searchQuery,
    filters,
    currentPage,
    totalItems,
    itemsPerPage,
    fetchData,
    goToPreviousPage,
    goToNextPage,
    pageCount
  }
}


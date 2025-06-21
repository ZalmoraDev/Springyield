// src/utils/api.js
import {getToken} from './auth';
import {API_BASE_URL} from '../config';
import {logout} from './auth';

/**
 * Wrapper around fetch that automatically includes authentication headers
 * and handles common fetch configurations
 *
 * @param {string} endpoint - The API endpoint (without the base URL)
 * @param {Object} options - Fetch options
 * @returns {Promise} - The fetch promise
 */
export async function apiFetch(endpoint, options = {}) {
    // Ensure headers object exists
    if (!options.headers) {
        options.headers = {};
    }

    // If headers is provided as Headers instance, convert to plain object
    if (options.headers instanceof Headers) {
        const headersObj = {};
        options.headers.forEach((value, key) => {
            headersObj[key] = value;
        });
        options.headers = headersObj;
    }

    // Get the token and add it to headers if available
    const token = getToken();
    if (token) {
        options.headers['Authorization'] = `Bearer ${token}`;
    }

    // Set CORS-related options to ensure proper cross-origin requests
    options.mode = 'cors';
    options.credentials = 'same-origin';

    // Ensure Content-Type is set for JSON requests
    if (options.body && typeof options.body !== 'string' && !(options.body instanceof FormData)) {
        options.headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(options.body);
    }

    // Construct the full URL (handle both relative and absolute endpoints)
    const url = endpoint.startsWith('http') ? endpoint : `${API_BASE_URL}${endpoint}`;

    // Call the native fetch with our enhanced options
    const response = await fetch(url, options);

    // Check for 401 Unauthorized response - token is likely expired or invalid
    if (response.status === 401) {
        logout();
        // Import router dynamically to avoid circular dependency
        const router = await import('../router/router').then(module => module.router);
        router.push('/login'); // Redirect to login page
        return Promise.reject(new Error('Session expired. Please log in again.'));
    }

    return response;
}

// Export default for easier importing
export default apiFetch;


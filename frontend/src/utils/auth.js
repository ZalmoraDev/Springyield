// src/utils/auth.js
import { jwtDecode } from 'jwt-decode'

export function getToken() {
    return localStorage.getItem('token')
}

export function getUserRole() {
    const token = getToken()
    if (!token) return null

    try {
        const decoded = jwtDecode(token)
        return decoded.role
    } catch (err) {
        return null
    }
}

export function isTokenExpired() {
    const token = getToken()
    if (!token) return true

    try {
        const decoded = jwtDecode(token)
        // exp is in seconds since epoch, current time is in milliseconds
        return decoded.exp * 1000 < Date.now()
    } catch (err) {
        return true // If we can't decode the token, consider it expired
    }
}

// Removes localStorage user & token, router.js handles routing back to /login if the token is expired
export function logout() {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
}

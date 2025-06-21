import {createRouter, createWebHistory} from "vue-router";
import {getToken, getUserRole, isTokenExpired, logout} from "@/utils/auth.js";
import {validRoutes, errorRoutes} from "@/router/routes.js";

export const router = createRouter({
    history: createWebHistory(),
    routes: [...validRoutes, ...errorRoutes]
})

//----------------------------------------------------------------------------------------------------------------------

router.beforeEach((to, from, next) => {
    const token = getToken()
    const requiredAuth = to.meta.requiresAuth // Does it require a JWT token?
    const allowedRoles = to.meta.allowedRoles // What user roles can access this route?

    // Check if token is expired and user is navigating to another page
    if (token && isTokenExpired()) {
        logout() // Clear token and user from localStorage
        return next('/login') // Redirect to login page
    }

    // If visiting root path and no token is present -> login
    if (to.path === '/' && !token)
        return next('/login')

    // If route requires auth & the user is not authenticated -> 401 error page (Niet geautoriseerd)
    if (requiredAuth && !token && to.path)
        return next('/error/401')

    // If logged in and visiting login or signup -> dashboard (/) [1] Admin/Employee [2] Approved [3] Unapproved
    if ((to.path === '/login' || to.path === '/signup') && token)
        return next('/')

    // If user is authenticated, but doesn't have the correct role -> 403 error page (Toegang geweigerd)
    if (allowedRoles) {
        if (!allowedRoles.includes(getUserRole()))
            return next('/error/403')
    }

    // If page doesn't require auth, or the user is authenticated and has the correct role -> continue
    return next()
});

router.dynamicPageData = {}

router.afterEach((to) => {
    // Check if the dynamicTitle function is given in the route meta to provide title, else default to page name
    let title;
    if (to.meta.dynamicTitle)
        title = to.meta.dynamicTitle(to, router.dynamicPageData[to.path])
    else
        title = to.meta.title || to.name
    document.title = `${title} | Springyield`
})

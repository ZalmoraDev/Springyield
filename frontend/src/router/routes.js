// Utils
import {formatNameWithInitial} from "@/utils/formatters.js";

// PublicViews | AUTH: False | ROLES: None
import Login from "@/views/Login.vue";
import Signup from "@/views/Signup.vue";

// AUTH: True | ROLES: All (Rendered DOM elements may be different based on role)
import Dashboard from "@/views/Dashboard.vue";

// AUTH: True | ROLES: ['UNAPPROVED', 'APPROVED']
import AccountDetail from "@/views/customerViews/AccountDetail.vue";
import AtmView from "@/views/customerViews/AtmView.vue";
import CreateTransaction from "@/views/customerViews/CreateTransaction.vue";
import Profile from "@/views/customerViews/Profile.vue";

// AUTH: True | ROLES: ['EMPLOYEE', 'ADMIN']
import Users from "@/views/adminViews/Users.vue";
import UserDetails from '@/views/adminViews/UserDetails.vue';
import Accounts from "@/views/adminViews/Accounts.vue";
import AccountDetails from '@/views/adminViews/AccountDetails.vue';
import Transactions from "@/views/adminViews/Transactions.vue";
import TransactionDetails from "@/views/adminViews/TransactionDetails.vue";
import MakeTransaction from "@/views/adminViews/MakeTransaction.vue";


export const validRoutes = [
    // PublicViews | AUTH: False | ROLES: None
    {
        path: '/login',
        name: 'Login',
        component: Login
    },
    {
        path: '/signup',
        name: 'Signup',
        component: Signup
    },

    // -----------------------------------------------------------------------------------------------------------------
    // AuthViews | AUTH: True | ROLES: All (Rendered DOM elements may be different based on role)
    {
        path: '/',
        name: 'Dashboard',
        component: Dashboard,
        meta: {requiresAuth: true, allowedRoles: ['UNAPPROVED', 'APPROVED', 'EMPLOYEE', 'ADMIN']}
    },

    // -----------------------------------------------------------------------------------------------------------------
    // CustomerViews | AUTH: True | ROLES: ['UNAPPROVED', 'APPROVED']
    {
        path: '/account/:iban',
        name: 'AccountDetail',
        component: AccountDetail,
        meta: {
            requiresAuth: true,
            allowedRoles: ['APPROVED'],
            dynamicTitle: (route, accountData) => {
                if (accountData && accountData.iban) {
                    return `${accountData.iban} | Springyield`
                }
                return 'Account Details'; // Fallback title
            }
        }
    },
    {
        path: '/profile',
        name: 'Profile',
        component: Profile,
        meta: {
            requiresAuth: true,
            allowedRoles: ['APPROVED']
        }
    },
    {
        path: '/atm',
        name: 'AtmView',
        component: AtmView,
        meta: {requiresAuth: true, allowedRoles: ['APPROVED']}
    },
    {
        path: '/create-transaction',
        name: 'Create Transaction',
        component: CreateTransaction,
        meta: {requiresAuth: true, allowedRoles: ['APPROVED']}
    },

    // -----------------------------------------------------------------------------------------------------------------
    // Admin Views | AUTH: True | ROLES: ['EMPLOYEE', 'ADMIN']
    // User management
    {
        path: '/admin/users',
        name: 'Users',
        component: Users,
        meta: {requiresAuth: true, allowedRoles: ['EMPLOYEE', 'ADMIN']}
    },
    {
        path: '/admin/user/:userId',
        name: 'UserDetails',
        component: UserDetails,
        meta: {
            requiresAuth: true,
            allowedRoles: ['EMPLOYEE', 'ADMIN'],
            dynamicTitle: (route, userData) => {
                // If userData available, format dynamic title
                if (userData?.firstName) {
                    const formattedName = formatNameWithInitial(userData.firstName, userData.lastName);
                    return `User: ${formattedName}`;
                }
                return 'User Details';  // Fallback title
            }
        }
    },

    // Account management
    {
        path: '/admin/accounts',
        name: 'Accounts',
        component: Accounts,
        meta: {requiresAuth: true, allowedRoles: ['EMPLOYEE', 'ADMIN']},
    },
    {
        path: '/admin/account/:accountId',
        name: 'AccountDetails',
        component: AccountDetails,
        meta: {
            requiresAuth: true,
            allowedRoles: ['EMPLOYEE', 'ADMIN'],
            dynamicTitle: (route, accountData) => {
                // If accountData available, format dynamic title
                if (accountData?.firstName) {
                    const formattedName = formatNameWithInitial(accountData.firstName, accountData.lastName);
                    return `Acc: ${accountData.accountId + ' ' + formattedName}`;
                }
                // Fallback to default title if firstName is missing or formattedName is empty
                return 'Account Details';
            }
        }
    },

    // Transaction management
    {
        path: '/admin/transactions',
        name: 'Transactions',
        component: Transactions,
        meta: {requiresAuth: true, allowedRoles: ['EMPLOYEE', 'ADMIN']},
    },
    {
        path: '/admin/transactions/:transactionId',
        name: 'TransactionsDetails',
        component: TransactionDetails,
        meta: {
            requiresAuth: true,
            allowedRoles: ['EMPLOYEE', 'ADMIN'],
            dynamicTitle: (route, transactionData) => {
                // If accountData available, format dynamic title
                if (transactionData?.transactionId) {
                    return `Trans: ${transactionData.transactionId}`;
                }
                // Fallback to default title if firstName is missing or formattedName is empty
                return 'Transaction Details';
            }
        }
    },
    {
        path: '/admin/transactions/create',
        name: 'AdminCreateTransaction',
        component: MakeTransaction,
        meta: {
            requiresAuth: true,
            allowedRoles: ['EMPLOYEE', 'ADMIN'],
            dynamicTitle: () => 'Admin| Create Transaction'
        }
    },
];

//----------------------------------------------------------------------------------------------------------------------
// Add catch-all route for 4xx errors

export const errorRoutes = [
    {
        path: '/error/:errorType',
        name: 'Error',
        component: () => import('@/views/ErrorPage.vue'),
        meta: {
            title: 'Error',
            dynamicTitle: (to) => `Error ${to.params.errorType}`
        }
    },
    {
        path: '/:pathMatch(.*)*',
        name: 'NotFound',
        redirect: to => {
            return {path: '/error/404'}
        }
    }
];

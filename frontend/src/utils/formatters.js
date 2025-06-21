/**
 * Utility functions for formatting data in a consistent way throughout the application
 */

/**
 * Formats a name as "F. Lastname" where F is the first letter of the first name
 * @param {string} firstName - The first name
 * @param {string} lastName - The last name
 * @returns {string} The formatted name
 */
export const formatNameWithInitial = (firstName, lastName) => {
    if (!firstName || !lastName) return '';

    return `${firstName.charAt(0).toUpperCase()}. ${lastName.charAt(0).toUpperCase()}${lastName.slice(1)}`;
};

/**
 * Formats a currency value with a plus/minus sign (or no sign for zero) and a Euro symbol.
 * @param {number} balance - The balance to format.
 * @returns {string} The formatted currency string (e.g., "+€1.234,56", "-€1.234,56", or "€0,00").
 */
export const formatCurrency = (balance) => {
    if (typeof balance !== 'number') return '';

    let sign = '';
    if (balance < 0) {
        sign = '-';
    } else if (balance > 0) {
        sign = '+';
    }

    const formattedBalance = Math.abs(balance).toLocaleString('nl-NL', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });

    return `${sign}€${formattedBalance}`;
};



/**
 * Formats a currency value with a minus sign and a Euro symbol, leaving out the plus sign for positive balances.
 * @param {number} balance - The balance to format.
 * @returns {string} The formatted currency string (e.g., "€1.234,56" or "-€1.234,56").
 */
export const formatCurrencyNoPlus = (balance) => {
    if (typeof balance !== 'number') return '';

    const sign = balance < 0 ? '-' : '';
    const formattedBalance = Math.abs(balance).toLocaleString('nl-NL', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });

    return `${sign}€${formattedBalance}`;
};

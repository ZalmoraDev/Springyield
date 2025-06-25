package com.stefvisser.springyield.services;

import jakarta.transaction.Transactional;
import com.stefvisser.springyield.dto.AccountProfileDto;
import com.stefvisser.springyield.dto.PaginatedDataDto;
import com.stefvisser.springyield.models.Account;
import com.stefvisser.springyield.models.AccountStatus;
import com.stefvisser.springyield.models.AccountType;
import com.stefvisser.springyield.models.Transaction;
import com.stefvisser.springyield.models.User;
import com.stefvisser.springyield.repositories.AccountRepository;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    public AccountServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // API Methods
    // -----------------------------------------------------------------------------------------------------------------
    /**
     * Searches for accounts based on specified criteria with pagination.
     * <p>
     * This method allows filtering accounts by a search query, account type, and status,
     * with results returned in a paginated format for efficient data transfer.
     * </p>
     *
     * @param execUser the user executing the request
     * @param query optional search string to filter accounts by various attributes
     * @param accountType optional account type filter (e.g., CURRENT, SAVINGS)
     * @param status optional account status filter (e.g., ACTIVE, CLOSED)
     * @param limit maximum number of results per page (defaults to 10)
     * @param offset starting position for pagination (defaults to 0)
     * @return PaginatedDataDto containing paginated account search results
     */
    public PaginatedDataDto<AccountProfileDto> search(User execUser, String query, AccountType accountType, AccountStatus status, int limit, int offset) {
        if (execUser == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");

        if (!execUser.isEmployee())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to view accounts");

        if (limit <= 0) limit = 10;
        if (offset < 0) offset = 0;
        if (query == null) query = "";

        return accountRepository.searchAccount(query, accountType, status, limit, offset);
    }

    /**
     * Retrieves an account by its IBAN.
     * <p>
     * This method allows users to retrieve account details using the IBAN.
     * </p>
     *
     * @param execUser the user executing the request
     * @param iban the IBAN of the account to retrieve
     * @return the Account object associated with the given IBAN
     */
    public Account getAccountByIban(User execUser, String iban) {
        if (execUser == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");

        Account account = accountRepository.findByIban(iban);
        if (account == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found with IBAN: " + iban);

        if (!Objects.equals(account.getUser().getUserId(), execUser.getUserId()) && !execUser.isEmployee())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to view this account");

        return account;
    }

    /**
     * Updates the balance limits for a specific account.
     * <p>
     * This method allows an employee to update the daily and absolute limits for an account.
     * </p>
     *
     * @param execUser the user executing the request (must be an employee)
     * @param accountId the ID of the account to update
     * @param dailyLimit the new daily limit for the account
     * @param absoluteLimit the new absolute limit for the account
     * @return the updated Account object
     */
    @Transactional
    public Account updateBalanceLimits(User execUser, Long accountId, BigDecimal dailyLimit, BigDecimal absoluteLimit) {
        if (execUser == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");

        if (!execUser.isEmployee())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to update account limits");

        Account account = accountRepository.findByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        account.setDailyLimit(dailyLimit);
        account.setAbsoluteLimit(absoluteLimit);

        return accountRepository.save(account);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Non-API Methods (Less authentication required, since they are used internally)
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * ONLY USED EXTERNALLY: Creates a new account for a user with the specified parameters.
     * <p>
     * This method generates a random IBAN in the NL SPYD format and initializes the account with the provided
     * parameters. The account is set to ACTIVE status and has an empty transaction list.
     * </p>
     *
     * @param user the user for whom the account is being created
     * @param accountType the type of account (e.g., CURRENT, SAVINGS)
     * @param dailyLimit the daily transaction limit for the account
     * @param absoluteLimit the absolute limit for the account
     * @param initialBalance the initial balance of the account
     * @param balanceLimit the balance limit for the account
     * @return the created Account object
     */
    public Account createAccount(User user, AccountType accountType, BigDecimal dailyLimit,
                                 BigDecimal absoluteLimit, BigDecimal initialBalance, BigDecimal balanceLimit) {
        // Generate NL SPYD format IBAN
        String iban = new Iban.Builder()
                .countryCode(CountryCode.NL)
                .bankCode("SPYD")
                .buildRandom()
                .toFormattedString();

        // Create account with defaults for id, date, status, and transaction list
        Account account = new Account(
                null,         // id always null for new accounts
                user,
                iban,                  // generated IBAN
                LocalDate.now(),       // always current date (UNUSED IN FINAL BUILD)
                accountType,           // specified account type
                dailyLimit,
                absoluteLimit,
                initialBalance,
                balanceLimit,
                AccountStatus.ACTIVE,  // status always ACTIVE for new accounts
                new ArrayList<>()      // transaction list always empty for new accounts
        );

        return accountRepository.save(account);
    }

    /**
     * Updates an existing account in the repository.
     * <p>
     * This method is used to persist changes made to an account entity.
     * </p>
     *
     * @param account the account to be updated
     */
    public void updateAccount(Account account) {
        accountRepository.save(account);
    }

    /**
     * Saves a list of accounts to the repository.
     * <p>
     * This method is used to persist multiple account entities in a single operation.
     * </p>
     *
     * @param accounts the list of accounts to be saved
     */
    public void saveAll(List<Account> accounts) {
        accountRepository.saveAll(accounts);
    }
}

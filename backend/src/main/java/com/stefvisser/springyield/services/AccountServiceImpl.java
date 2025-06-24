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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    public AccountServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public List<Account> getAccountsByUserId(Long userId) {
        return accountRepository.findByUserUserId(userId);
    }

    public Account getAccountByIban(String iban) {
        return accountRepository.findByIban(iban);
    }

    public void updateAccount(Account account) {
        accountRepository.save(account);
    }

    /**
     * Creates a new account with standard defaults applied.
     * This simplified method only requires the essential parameters,
     * and applies default values for id (null), iban (NL SPYD format),
     * creation date (current date), transaction list (empty), and status (ACTIVE).
     *
     * @param user           the user who owns the account
     * @param accountType    the type of account (PAYMENT, SAVINGS, etc.)
     * @param dailyLimit     the daily transaction limit
     * @param absoluteLimit  the absolute transaction limit
     * @param initialBalance the starting balance
     * @param balanceLimit   the lower limit for the account balance
     * @return the saved account
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

    @Transactional
    public Account updateAccountLimits(Long accountId, BigDecimal dailyLimit, BigDecimal absoluteLimit) {
        Account account = accountRepository.findByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        account.setDailyLimit(dailyLimit);
        account.setAbsoluteLimit(absoluteLimit);

        return accountRepository.save(account);
    }

    @Override
    public PaginatedDataDto<AccountProfileDto> searchAccountByName(String query) {
        query = query.toLowerCase();
        query = query.trim();
        return accountRepository.searchAccount(query, AccountType.PAYMENT, null, 50, 0);
    }

    @Override
    public PaginatedDataDto<AccountProfileDto> searchAccount(String query, AccountType accountType, AccountStatus status, int limit, int offset) {
        return accountRepository.searchAccount(query, accountType, status, limit, offset);
    }

    @Override
    public boolean validateTransaction(Account account, Transaction transaction) {
        return false;
    }
}

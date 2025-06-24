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
import org.springframework.http.ResponseEntity;
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

    @Override
    public PaginatedDataDto<AccountProfileDto> search(String query) {
        query = query.toLowerCase();
        query = query.trim();
        return accountRepository.searchAccount(query, AccountType.PAYMENT, null, 50, 0);
    }

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

    public void updateAccount(Account account) {
        accountRepository.save(account);
    }

    @Transactional
    public Account updateBalanceLimits(Long accountId, BigDecimal dailyLimit, BigDecimal absoluteLimit) {
        Account account = accountRepository.findByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        account.setDailyLimit(dailyLimit);
        account.setAbsoluteLimit(absoluteLimit);

        return accountRepository.save(account);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Non-API Methods (Less authentication required, since they are used internally)
    // -----------------------------------------------------------------------------------------------------------------

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

    @Override
    public PaginatedDataDto<AccountProfileDto> searchAccount(String query, AccountType accountType, AccountStatus status, int limit, int offset) {
        return accountRepository.searchAccount(query, accountType, status, limit, offset);
    }

    @Override
    public boolean validateTransaction(Account account, Transaction transaction) {
        return false;
    }


    @Override
    public void saveAll(List<Account> accounts) {
        accountRepository.saveAll(accounts);
    }

    @Override
    public void save(Account account) {
        accountRepository.save(account);
    }
}

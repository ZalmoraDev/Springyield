package com.stefvisser.springyield.services;

import jakarta.transaction.Transactional;
import com.stefvisser.springyield.dto.AccountProfileDto;
import com.stefvisser.springyield.dto.PaginatedDataDto;
import com.stefvisser.springyield.models.Account;
import com.stefvisser.springyield.models.AccountStatus;
import com.stefvisser.springyield.models.AccountType;
import com.stefvisser.springyield.models.Transaction;
import com.stefvisser.springyield.repositories.AccountRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    public Account createAccount(Account account) {
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


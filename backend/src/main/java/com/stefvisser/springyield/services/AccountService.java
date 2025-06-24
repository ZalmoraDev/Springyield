package com.stefvisser.springyield.services;

import com.stefvisser.springyield.models.*;
import com.stefvisser.springyield.dto.AccountProfileDto;
import com.stefvisser.springyield.dto.PaginatedDataDto;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {
    List<Account> getAccountsByUserId(Long userId);
    List<Account> getAllAccounts();
    Account getAccountByIban(String iban);
    void updateAccount(Account account);
    Account createAccount(User user, AccountType accountType, BigDecimal dailyLimit,
                          BigDecimal absoluteLimit, BigDecimal initialBalance, BigDecimal balanceLimit);
    boolean validateTransaction(Account account, Transaction transaction);
    PaginatedDataDto<AccountProfileDto> search(String query);
    PaginatedDataDto<AccountProfileDto> searchAccount(String query, AccountType accountType, AccountStatus status, int limit, int offset);
    Account updateBalanceLimits(Long accountId, BigDecimal dailyLimit, BigDecimal absoluteLimit);


    // Added methods for DataSeeder
    void saveAll(List<Account> accounts);
    void save(Account account);
}

package com.stefvisser.springyield.services;

import com.stefvisser.springyield.models.*;
import com.stefvisser.springyield.dto.AccountProfileDto;
import com.stefvisser.springyield.dto.PaginatedDataDto;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {

    // API Methods
    Account getAccountByIban(User execUser, String iban);
//    Account getAccountProfileDtoByIban(User execUser, String iban);
    void updateAccount(Account account);
    Account createAccount(User user, AccountType accountType, BigDecimal dailyLimit,
                          BigDecimal absoluteLimit, BigDecimal initialBalance, BigDecimal balanceLimit);
    boolean validateTransaction(Account account, Transaction transaction);
    PaginatedDataDto<AccountProfileDto> search(String query);
    PaginatedDataDto<AccountProfileDto> searchAccount(String query, AccountType accountType, AccountStatus status, int limit, int offset);
    Account updateBalanceLimits(Long accountId, BigDecimal dailyLimit, BigDecimal absoluteLimit);


    // Non-API Methods (Less authentication required, since they are used internally)
    void save(Account account);
    void saveAll(List<Account> accounts);
}

package com.stefvisser.springyield.services;

import com.stefvisser.springyield.models.*;
import com.stefvisser.springyield.dto.AccountProfileDto;
import com.stefvisser.springyield.dto.PaginatedDataDto;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {

    // API Methods
    PaginatedDataDto<AccountProfileDto> search(User execUser, String query, AccountType accountType, AccountStatus status, int limit, int offset);
    Account getAccountByIban(User execUser, String iban);
    Account updateBalanceLimits(User execUser, Long accountId, BigDecimal dailyLimit, BigDecimal absoluteLimit, BigDecimal balanceLimit);


    // Non-API Methods (Less authentication required, since they are used internally)
    Account createAccount(User user, AccountType accountType, BigDecimal dailyLimit,
                          BigDecimal absoluteLimit, BigDecimal initialBalance, BigDecimal balanceLimit);
    void updateAccount(Account account);
    void saveAll(List<Account> accounts);
}

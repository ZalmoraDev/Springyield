package com.stefvisser.springyield.services;

import com.stefvisser.springyield.dto.AddressBookEntryDTO;
import com.stefvisser.springyield.models.Account;
import com.stefvisser.springyield.models.AccountStatus;
import com.stefvisser.springyield.models.AccountType;
import com.stefvisser.springyield.models.Transaction;
import com.stefvisser.springyield.dto.AccountProfileDto;
import com.stefvisser.springyield.dto.PaginatedDataDTO;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {
    List<Account> getAccountsByUserId(Long userId);
    List<Account> getAllAccounts();
    Account getAccountByIban(String iban);
    void updateAccount(Account account);
    Account createAccount(Account account);
    boolean validateTransaction(Account account, Transaction transaction);
    PaginatedDataDTO<AccountProfileDto> searchAccount(String query, AccountType accountType, AccountStatus status, int limit, int offset);
    Account updateAccountLimits(Long accountId, BigDecimal dailyLimit, BigDecimal absoluteLimit);
    PaginatedDataDTO<AccountProfileDto> searchAccountByName(String query);
}

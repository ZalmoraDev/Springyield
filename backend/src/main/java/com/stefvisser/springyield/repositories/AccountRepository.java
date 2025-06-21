package com.stefvisser.springyield.repositories;

import com.stefvisser.springyield.models.*;
import com.stefvisser.springyield.dto.AccountProfileDto;
import com.stefvisser.springyield.dto.PaginatedDataDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    /// Return status on whether a generated IBAN number exists in the database
    /// when creating an account. Service loops until unique IBAN is generated.
    boolean existsByIban(String iban);

    List<Account> findByUserUserId(Long userId);

    List<Account> findAll();
    Optional<Account> findByAccountId(Long accountId);
    Account findByIban(String iban);

    default PaginatedDataDTO<AccountProfileDto> searchAccount(String query, AccountType accountType, AccountStatus status, int limit, int offset) {
        if (limit <= 0 || offset < 0)
            throw new IllegalArgumentException("Limit must be greater than 0 and offset must be non-negative.");
        if (query == null) query = "";

        String queryLower = query
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "");

        List<Account> accounts = this.findAll();
        Stream<Account> accountStream = accounts.stream();

        // Filter by account type if provided
        if (accountType != null) {
            accountStream = accountStream.filter(account ->
                    account.getAccountType() == accountType);
        }

        // Filter by account status if provided
        if (status != null) {
            accountStream = accountStream.filter(account ->
                    account.getStatus() == status);
        }

        // Apply IBAN search filter if query is not blank
        if (!query.isBlank()) {
            accountStream = accountStream.filter(account ->
                    account.getIban()
                            .toLowerCase(Locale.ROOT)
                            .replaceAll("\\s+", "")
                            .contains(queryLower)
                    || account.getUser()
                            .getFirstName()
                            .toLowerCase()
                            .contains(queryLower)
                    || account.getUser()
                            .getLastName()
                            .toLowerCase()
                            .contains(queryLower)
            );
        }
        List<Account> filteredAccounts = accountStream.toList();
        int totalCount = filteredAccounts.size();

        List<AccountProfileDto> paginatedAccounts = filteredAccounts.stream()
                .sorted(Comparator.comparing(Account::getAccountId).reversed())
                .skip(offset)
                .limit(limit)
                .map(AccountProfileDto::new)
                .toList();
        return new PaginatedDataDTO<>(paginatedAccounts, totalCount);
    }
}


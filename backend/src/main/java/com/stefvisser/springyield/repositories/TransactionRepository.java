package com.stefvisser.springyield.repositories;

import com.stefvisser.springyield.models.Transaction;
import com.stefvisser.springyield.dto.PaginatedDataDto;
import com.stefvisser.springyield.dto.TransactionDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Transaction findByTransactionId(Long transactionId);

    List<Transaction> findByFromAccount(String fromAccount);
    List<Transaction> findByReference(String reference);
    List<Transaction> findByFromAccountOrToAccount(String fromAccount, String toAccount);
    List<Transaction> findAll();

    default PaginatedDataDto<TransactionDto> searchTransactions(
            String searchQuery,
            String type,
            LocalDateTime startDate,
            LocalDateTime endDate,
            BigDecimal amountFrom,
            BigDecimal amountTo,
            String amountOperator,
            int limit,
            int offset) {

        if (limit <= 0 || offset < 0)
            throw new IllegalArgumentException("Limit must be greater than 0 and offset must be non-negative.");

        final String query = searchQuery == null ? "" : searchQuery.trim();
        final String queryLower = query.toLowerCase(Locale.ROOT);

        List<Transaction> transactions = this.findAll();
        Stream<Transaction> transactionStream = transactions.stream();

        // Search query filter (IBAN and other fields)
        if (!query.isBlank()) {
            transactionStream = transactionStream.filter(transaction ->
                    (transaction.getFromAccount() != null &&
                            transaction.getFromAccount().toLowerCase(Locale.ROOT).contains(queryLower)) ||
                            (transaction.getToAccount() != null &&
                                    transaction.getToAccount().toLowerCase(Locale.ROOT).contains(queryLower)) ||
                            (transaction.getReference() != null &&
                                    transaction.getReference().toLowerCase(Locale.ROOT).contains(queryLower)) ||
                            (transaction.getDescription() != null &&
                                    transaction.getDescription().toLowerCase(Locale.ROOT).contains(queryLower)) ||
                            String.valueOf(transaction.getTransactionId()).contains(queryLower)
            );
        }

        // Type filter
        if (type != null && !type.isBlank()) {
            transactionStream = transactionStream.filter(transaction ->
                    transaction.getTransactionType() != null &&
                            transaction.getTransactionType().toString().equalsIgnoreCase(type));
        }

        // Amount range filter
        if (amountOperator != null && !amountOperator.isBlank() && amountFrom != null) {
            transactionStream = transactionStream.filter(transaction -> {
                if (transaction.getTransferAmount() == null) return false;
                BigDecimal transactionAmount = transaction.getTransferAmount().abs();
                BigDecimal filterAmount = amountFrom.abs();

                return switch (amountOperator.toLowerCase()) {
                    case "lt" -> transactionAmount.compareTo(filterAmount) < 0;
                    case "gt" -> transactionAmount.compareTo(filterAmount) > 0;
                    case "eq" -> transactionAmount.compareTo(filterAmount) == 0;
                    default -> true;
                };
            });
        }

        // Date range filter
        // Update the date filter section in searchTransactions method
        // Date range filter
        if (startDate != null && endDate != null) {
            transactionStream = transactionStream.filter(transaction ->
                    transaction.getTimestamp() != null &&
                            !transaction.getTimestamp().isBefore(startDate) &&
                            !transaction.getTimestamp().isAfter(endDate)
            );
        }

        List<Transaction> filteredTransactions = transactionStream.toList();
        int totalCount = filteredTransactions.size();

        // Apply pagination and sorting
        List<TransactionDto> paginatedTransactionDTOs = filteredTransactions.stream()
                .sorted(Comparator.comparing(Transaction::getTimestamp, Comparator.nullsLast(Comparator.reverseOrder())))
                .skip(offset)
                .limit(limit)
                .map(TransactionDto::wrap)
                .toList();

        return new PaginatedDataDto<>(paginatedTransactionDTOs, totalCount);
    }
}



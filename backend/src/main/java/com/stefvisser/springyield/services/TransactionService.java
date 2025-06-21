package com.stefvisser.springyield.services;

import com.stefvisser.springyield.dto.TransactionDTO;
import com.stefvisser.springyield.dto.PaginatedDataDTO;
import com.stefvisser.springyield.models.Account;
import com.stefvisser.springyield.models.Transaction;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionService {
    List<Transaction> getAllTransactions();
    Transaction getTransactionById(long id);
    List<Transaction> getTransactionsByReference(String reference);
    List<Transaction> getTransactionsByIBAN(String iban);

    Transaction createTransaction(TransactionDTO transaction) throws ResponseStatusException;
    PaginatedDataDTO<TransactionDTO> searchTransactions(
            String query,
            String type,
            int limit,
            int offset,
            LocalDateTime startDate,
            LocalDateTime endDate,
            BigDecimal amountFrom,
            BigDecimal amountTo,
            String amountOperator
    );

    Transaction processAtmTransaction(TransactionDTO transactionDTO);
}

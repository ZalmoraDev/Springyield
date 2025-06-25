package com.stefvisser.springyield.services;

import com.stefvisser.springyield.dto.TransactionRequestDto;
import com.stefvisser.springyield.dto.PaginatedDataDto;
import com.stefvisser.springyield.models.Transaction;
import com.stefvisser.springyield.models.User;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionService {

    // API Methods
    PaginatedDataDto<TransactionRequestDto> searchTransactions(
            User execUser,
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
    Transaction getTransactionById(User execUser, long id);
    List<Transaction> getTransactionsByIban(User execUser, String iban);
    List<Transaction> getTransactionsByReference(User execUser, String reference);
    List<Transaction> getAllTransactions(User execUser);
    Transaction createTransaction(User execUser, TransactionRequestDto transaction) throws ResponseStatusException;
    Transaction createAtmTransaction(User execUser, TransactionRequestDto transactionReqDTO);


    // Non-API Methods (Less authentication required, since they are used internally)
    void saveAll(List<Transaction> transactions);
}

package com.stefvisser.springyield.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.stefvisser.springyield.models.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for transaction data.
 * <p>
 * This class represents transaction data transferred between the service layer and client.
 * It contains all the essential information about a transaction in the banking system,
 * such as the source and destination accounts, transfer amount, and transaction metadata.
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionDto {
    /**
     * The unique identifier for the transaction.
     */
    private Long transactionId;

    /**
     * The account number from which the funds are transferred.
     */
    private String fromAccount;

    /**
     * The account number to which the funds are transferred.
     */
    private String toAccount;

    /**
     * The amount of money transferred in the transaction.
     */
    private BigDecimal transferAmount;

    /**
     * A user-provided description of the transaction.
     */
    private String description;

    /**
     * A unique reference code for the transaction.
     */
    private String reference;

    /**
     * The date and time when the transaction occurred.
     */
    private LocalDateTime timestamp;

    /**
     * The type of transaction (e.g., deposit, withdrawal, transfer).
     */
    private Transaction.TransactionType transactionType;

    /**
     * Converts a Transaction model object to a TransactionDTO.
     * <p>
     * This static factory method creates a new TransactionDTO instance that contains
     * all the relevant data from the provided Transaction entity.
     * </p>
     *
     * @param transaction The Transaction entity to be wrapped
     * @return A new TransactionDTO containing data from the provided transaction
     */
    public static TransactionDto wrap(Transaction transaction) {
        return new TransactionDto(
                transaction.getTransactionId(),
                transaction.getFromAccount(),
                transaction.getToAccount(),
                transaction.getTransferAmount(),
                transaction.getDescription(),
                transaction.getReference(),
                transaction.getTimestamp(),
                transaction.getTransactionType()
        );
    }

    public static TransactionDto fromTransaction(Transaction transaction) {
        return new TransactionDto(
                transaction.getTransactionId(),
                transaction.getFromAccount(),
                transaction.getToAccount(),
                transaction.getTransferAmount(),
                transaction.getDescription(),
                transaction.getReference(),
                transaction.getTimestamp(),
                transaction.getTransactionType()
        );
    }
}

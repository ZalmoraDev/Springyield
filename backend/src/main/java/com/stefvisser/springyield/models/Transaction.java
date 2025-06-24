package com.stefvisser.springyield.models;

import jakarta.persistence.*;
import lombok.*;
import com.stefvisser.springyield.dto.TransactionRequestDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction implements Cloneable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "description")
    private String description;

    @Column(name = "reference")
    private String reference;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "to_account")
    private String toAccount;

    @Column(name = "from_account")
    private String fromAccount;

    @Column(name = "transfer_amount")
    private BigDecimal transferAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type")
    private TransactionType transactionType;

    public static Transaction fromDTO(TransactionRequestDto transactionReqDTO) {
        Transaction transaction = new Transaction();
        transaction.setFromAccount(transactionReqDTO.getFromAccString());
        transaction.setToAccount(transactionReqDTO.getToAccString());
        transaction.setTransferAmount(transactionReqDTO.getTransferAmount());
        transaction.setDescription(transactionReqDTO.getDescription());
        transaction.setReference(transactionReqDTO.getReference());
        transaction.setTimestamp(transactionReqDTO.getTimestamp());
        transaction.setTransactionType(transactionReqDTO.getTransactionType());
        return transaction;
    }

    @Override
    public Transaction clone() {
        try {
            Transaction clone = (Transaction) super.clone();
            clone.setTimestamp(LocalDateTime.from(this.timestamp)); // Clone the timestamp to avoid shared reference
            clone.setDescription(this.description != null ? new String(this.description) : null);
            clone.setReference(this.reference != null ? new String(this.reference) : null);
            clone.setFromAccount(this.fromAccount != null ? new String(this.fromAccount) : null);
            clone.setToAccount(this.toAccount != null ? new String(this.toAccount) : null);
            clone.setTransferAmount(this.transferAmount != null ? new BigDecimal(this.transferAmount.toString()) : null);
            clone.setTransactionType(this.transactionType);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

}

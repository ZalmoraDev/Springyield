package com.stefvisser.springyield.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.stefvisser.springyield.dto.AccountProfileDto;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accountId;

    /// JsonIgnore prevents infinite recursion of JPA trying to add a User object when automatically serializing
    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @Column(unique = true)
    private String iban;

    private LocalDate registrationDate;

    @Enumerated(EnumType.STRING)
    private AccountType accountType;

    /// max amount of money that can be transferred in a single day (0:00 - 23:59)
    private BigDecimal dailyLimit;

    /// max amount of money that can be transferred in a single transaction
    private BigDecimal absoluteLimit;

    /// current balance of the account
    private BigDecimal balance;

    /// max negative balance allowed
    private BigDecimal balanceLimit;

    /// indicates if this account is active or has been deactivated (e.g., after user deletion)
    private AccountStatus status;

    @JsonIgnore
    @OneToMany(mappedBy = "fromAccount", cascade = CascadeType.ALL)
    private List<Transaction> transactions = new ArrayList<>();
}

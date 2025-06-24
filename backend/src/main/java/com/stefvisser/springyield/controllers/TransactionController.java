package com.stefvisser.springyield.controllers;

import com.stefvisser.springyield.dto.TransactionRequestDto;
import com.stefvisser.springyield.dto.PaginatedDataDto;
import com.stefvisser.springyield.models.Account;
import com.stefvisser.springyield.models.Transaction;
import com.stefvisser.springyield.models.TransactionType;
import com.stefvisser.springyield.models.User;
import com.stefvisser.springyield.services.TransactionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchTransactions(
            @AuthenticationPrincipal User execUser,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) BigDecimal amountFrom,
            @RequestParam(required = false) BigDecimal amountTo,
            @RequestParam(required = false) String amountOperator
    ) {
        try {
            PaginatedDataDto<TransactionRequestDto> transactions = transactionService.searchTransactions(
                    execUser, query, type, limit, offset, startDate, endDate, amountFrom, amountTo, amountOperator
            );
            return ResponseEntity.ok(transactions);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    @GetMapping("/id/{transactionID}")
    public ResponseEntity<?> getTransactionByID(@AuthenticationPrincipal User execUser, @PathVariable Long transactionID) {
        try {
            Transaction transaction = transactionService.getTransactionById(execUser, transactionID);
            return ResponseEntity.ok(TransactionRequestDto.wrap(transaction));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getBody());
        }
    }

    @GetMapping("/iban/{iban}")
    public ResponseEntity<?> getTransactionsByIBAN(@AuthenticationPrincipal User execUser, @PathVariable String iban) {
        try {
            List<Transaction> transactions = transactionService.getTransactionsByIBAN(execUser, iban);

            // TODO: TransactionDto is literally just a Transaction with a static wrap method, just make it  Transaction
            return ResponseEntity.ok(transactions.stream()
                    .map(TransactionRequestDto::wrap)
                    .toList());
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getBody());
        }
    }

    @GetMapping("/reference/{reference}")
    public ResponseEntity<?> getTransactionsByReference(@AuthenticationPrincipal User execUser, @PathVariable String reference) {
        try {
            List<Transaction> transactions = transactionService.getTransactionsByReference(execUser, reference);

            // TODO: TransactionDto is literally just a Transaction with a static wrap method, just make it  Transaction
            return ResponseEntity.ok(transactions
                    .stream()
                    .map(TransactionRequestDto::wrap)
                    .toList());
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getBody());
        }
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionRequestDto>> getAllTransactions(@AuthenticationPrincipal User execUser) {
        List<Transaction> transactions = transactionService.getAllTransactions(execUser);

        // TODO: TransactionDto is literally just a Transaction with a static wrap method, just make it  Transaction
        return ResponseEntity.ok(transactions
                .stream()
                .map(TransactionRequestDto::wrap)
                .toList());
    }

    @PostMapping("/create")
    public ResponseEntity<?> createTransaction(@AuthenticationPrincipal User execUser, @RequestBody() TransactionRequestDto transactionReqDTO) {
        try {
            Transaction transaction = transactionService.createTransaction(execUser, transactionReqDTO);
            // TODO: TransactionDto is literally just a Transaction with a static wrap method, just make it Transaction
            return ResponseEntity.status(HttpStatus.CREATED).body(TransactionRequestDto.wrap(transaction));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    @PostMapping("/atm")
    public ResponseEntity<?> processAtmTransaction(@AuthenticationPrincipal User execUser, @RequestBody TransactionRequestDto body) {
        // Retrieve the account by IBAN from the request
        Account fromAccount = accountService.getAccountByIban(execUser, body.getFromAccString());

        // If the account does not exist, return 404
        if (fromAccount == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account not found");
        }

        // Set both from and to account to the same account for ATM transactions
        // For ATM transactions, set both from and to account to the same IBAN initially
        body.setToAccString(body.getFromAccString());

        // Ensure the account is associated with a user
        if (fromAccount.getUser() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid account: no user associated");
        }

        // Retrieve the special ATM user by email
        User executingUser = userService.findByEmail("atms@springyield.com");

        // If the ATM user does not exist, return 403
        if (executingUser == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("ATM user not found or not authorized to perform this transaction");
        }

        // Adjust the from/to account based on transaction type
        if (body.getTransactionType().equals(TransactionType.DEPOSIT)) {
            // For deposits, money comes from the ATM user to the account
            body.setFromAccString(executingUser.getAccounts().getFirst().getIban());
        } else if (body.getTransactionType().equals(TransactionType.WITHDRAW)) {
            // For withdrawals, money goes from the account to the ATM user
            body.setToAccString(executingUser.getAccounts().getFirst().getIban());
        } else {
            // Invalid transaction type for ATM
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid transaction type for ATM transaction");
        }

        // Delegate to the general transaction creation logic
        return createTransaction(executingUser, body);
    }
}

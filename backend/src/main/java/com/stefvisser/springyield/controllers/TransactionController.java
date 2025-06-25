package com.stefvisser.springyield.controllers;

import com.stefvisser.springyield.dto.TransactionRequestDto;
import com.stefvisser.springyield.dto.PaginatedDataDto;
import com.stefvisser.springyield.models.Transaction;
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
            PaginatedDataDto<TransactionRequestDto> paginatedTransactions = transactionService.searchTransactions(
                    execUser, query, type, limit, offset, startDate, endDate, amountFrom, amountTo, amountOperator
            );
            return ResponseEntity.ok(paginatedTransactions);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    @GetMapping("/id/{transactionID}")
    public ResponseEntity<?> getTransactionById(@AuthenticationPrincipal User execUser, @PathVariable Long transactionID) {
        try {
            Transaction transaction = transactionService.getTransactionById(execUser, transactionID);
            return ResponseEntity.ok(TransactionRequestDto.wrap(transaction));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getBody());
        }
    }

    @GetMapping("/iban/{iban}")
    public ResponseEntity<?> getTransactionsByIban(@AuthenticationPrincipal User execUser, @PathVariable String iban) {
        try {
            List<Transaction> transactions = transactionService.getTransactionsByIban(execUser, iban);

            // Instead of sending Account data, send only the account IBANs instead
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

            // Instead of sending Account data, send only the account IBANs instead
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

        // Instead of sending Account data, send only the account IBANs instead
        return ResponseEntity.ok(transactions
                .stream()
                .map(TransactionRequestDto::wrap)
                .toList());
    }

    @PostMapping("/create")
    public ResponseEntity<?> createTransaction(@AuthenticationPrincipal User execUser, @RequestBody() TransactionRequestDto transactionReqDTO) {
        try {
            Transaction transaction = transactionService.createTransaction(execUser, transactionReqDTO);
            // Instead of sending Account data, send only the account IBANs instead
            return ResponseEntity.status(HttpStatus.CREATED).body(TransactionRequestDto.wrap(transaction));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    @PostMapping("/atm")
    public ResponseEntity<?> createAtmTransaction(@AuthenticationPrincipal User execUser, @RequestBody TransactionRequestDto transactionReqDto) {
        try {
            Transaction transaction = transactionService.createAtmTransaction(execUser, transactionReqDto);
            // Instead of sending Account data, send only the account IBANs instead
            return ResponseEntity.status(HttpStatus.CREATED).body(TransactionRequestDto.wrap(transaction));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }
}
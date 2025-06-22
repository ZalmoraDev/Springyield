package com.stefvisser.springyield.controllers;

import com.stefvisser.springyield.dto.TransactionDto;
import com.stefvisser.springyield.dto.PaginatedDataDto;
import com.stefvisser.springyield.models.Account;
import com.stefvisser.springyield.models.Transaction;
import com.stefvisser.springyield.models.User;
import com.stefvisser.springyield.services.AccountService;
import com.stefvisser.springyield.services.TransactionService;
import com.stefvisser.springyield.services.UserService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for managing banking transactions.
 * <p>
 * This controller provides endpoints for retrieving, searching, and creating transactions.
 * It handles HTTP requests related to transaction operations and delegates the business logic
 * to the appropriate service classes. All endpoints are mapped to the "/api/transactions" base path.
 * </p>
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final TransactionService transactionService;
    private final AccountService accountService;
    private final UserService userService;

    /**
     * Constructs a new TransactionController with the required services.
     *
     * @param transactionService service for transaction-related operations
     * @param accountService     service for account-related operations
     */
    public TransactionController(TransactionService transactionService, AccountService accountService, UserService userService) {
        this.transactionService = transactionService;
        this.accountService = accountService;
        this.userService = userService;
    }

    /**
     * Retrieves all transactions in the system.
     * <p>
     * This endpoint returns a complete list of all transactions and is typically
     * accessible only to employees or administrators.
     * </p>
     *
     * @return ResponseEntity containing a list of all transactions as DTOs
     */
    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionDto>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions()
                .stream()
                .map(TransactionDto::wrap)
                .toList());
    }

    /**
     * Creates a new transaction based on the provided transaction data.
     * <p>
     * This endpoint allows authenticated users to create a transaction between accounts.
     * It validates the user's access to the specified accounts and ensures that the
     * fromAccount and toAccount are not the same.
     * </p>
     *
     * @param user           the authenticated user initiating the transaction
     * @param transactionDTO the transaction data to create
     * @return ResponseEntity containing the created transaction as DTO
     */
    @PostMapping("/create")
    public ResponseEntity<?> createTransaction(@AuthenticationPrincipal User user, @RequestBody() TransactionDto transactionDTO) {
        try {
            if (user == null)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");

            // 4 uur werk om deze enkele regel toe te moeten voegen.
            // User user werkt niet want een authenticatedPrincipal User is niet een user entity...
            User accountOwner = userService.getUserById(user.getUserId());

            if (transactionDTO.getFromAccount() == null || transactionDTO.getToAccount() == null)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both from account and to account must be provided");

            String fromAccount = transactionDTO.getFromAccount().replace(" ", "").toLowerCase();
            String toAccount = transactionDTO.getToAccount().replace(" ", "").toLowerCase();

            if (fromAccount.equalsIgnoreCase(toAccount))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "From and To accounts cannot be the same");

            if (!accountOwner.hasAccount(fromAccount) && !accountOwner.isEmployee())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User does not have access to the specified accounts");

            Transaction transaction = transactionService.createTransaction(transactionDTO);

            return ResponseEntity.status(HttpStatus.CREATED).body(TransactionDto.wrap(transaction));
        } catch (ResponseStatusException e) {
            // Handle specific exceptions and return appropriate HTTP status codes
            return ResponseEntity.status(e.getStatusCode()).body(e.getBody());
        } catch (Exception e) {
            // Handle general exceptions and return a 500 Internal Server Error
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    "An error occurred while processing the transaction: " + e.getMessage()
            );
        }
    }

    /**
     * Searches for transactions based on specified criteria with pagination.
     * <p>
     * This endpoint allows filtering transactions by a search query and transaction type,
     * with results returned in a paginated format for efficient data transfer.
     * </p>
     *
     * @param query  optional search string to filter transactions by various attributes
     * @param type   optional transaction type filter
     * @param limit  maximum number of results per page (defaults to 10)
     * @param offset starting position for pagination (defaults to 0)
     * @return ResponseEntity containing paginated transaction search results
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchTransactions(
            @AuthenticationPrincipal User user,
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
            if (user == null)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");

            User executingUser = userService.getUserById(user.getUserId());
            if (!executingUser.isEmployee()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to search transactions");
            }


            if (limit <= 0) limit = 10;
            if (offset < 0) offset = 0;
            if (query == null) query = "";

            PaginatedDataDto<TransactionDto> transactions = transactionService.searchTransactions(
                    query, type, limit, offset, startDate, endDate, amountFrom, amountTo, amountOperator
            );

            return ResponseEntity.ok(transactions);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getBody());
        }

    }

    /**
     * Retrieves all transactions associated with a specific account IBAN.
     * <p>
     * This endpoint returns transactions where the specified IBAN is either
     * the source or destination account.
     * </p>
     *
     * @param iban the International Bank Account Number to retrieve transactions for
     * @return ResponseEntity containing a list of transactions for the specified IBAN
     */
    @GetMapping("/iban/{iban}")
    public ResponseEntity<?> getTransactionsByIBAN(@AuthenticationPrincipal User user, @PathVariable String iban) {
        try {

            if (user == null)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");

            // First check if the account exists
            Account account = accountService.getAccountByIban(iban);
            if (account == null)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account with IBAN " + iban + " not found");

            // Check if user is employee or owns the account by comparing user IDs
            boolean hasAccess = user.isEmployee() ||
                    (account.getUser() != null &&
                            account.getUser().getUserId().equals(user.getUserId()));

            if (!hasAccess)
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not have access to the specified account");

            List<Transaction> transactions = transactionService.getTransactionsByIBAN(iban);

            // At this point, the user either owns the account or is an employee, so they have permission to view the transactions
            return ResponseEntity.ok(transactions.stream()
                    .map(TransactionDto::wrap)
                    .toList());
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getBody());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }

    /**
     * Retrieves all transactions with a specific reference code.
     * <p>
     * This endpoint allows finding transactions that share the same reference,
     * which can be useful for tracking related transactions.
     * </p>
     *
     * @param reference the transaction reference code to search for
     * @return ResponseEntity containing a list of transactions with the specified reference
     */
    @GetMapping("/reference/{reference}")
    public ResponseEntity<?> getTransactionsByReference(@AuthenticationPrincipal User user, @PathVariable String reference) {
        try {
            if (user == null)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");

            User executingUser = userService.getUserById(user.getUserId());

            if (!executingUser.isEmployee()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to search transactions by reference");
            }
            return ResponseEntity.ok(transactionService.getTransactionsByReference(reference)
                    .stream()
                    .map(TransactionDto::wrap)
                    .toList());
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getBody());
        }
    }

    /**
     * Retrieves a single transaction by its unique identifier.
     * <p>
     * This endpoint returns detailed information about a specific transaction.
     * If no transaction with the given ID exists, a 404 Not Found response is returned.
     * </p>
     *
     * @param transactionID the unique identifier of the transaction
     * @return ResponseEntity containing the transaction if found, or a not found status
     */
    @GetMapping("/id/{transactionID}")
    public ResponseEntity<?> getTransactionByID(@AuthenticationPrincipal User user, @PathVariable Long transactionID) {
        try {
            if (user == null)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");

            User executingUser = userService.getUserById(user.getUserId());

            if (!executingUser.isEmployee()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to view this transaction");
            }

            Transaction transaction = transactionService.getTransactionById(transactionID);
            if (transaction == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(TransactionDto.wrap(transaction));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getBody());
        }
    }

    /**
     * Processes an ATM transaction (deposit or withdrawal).
     * <p>
     * This endpoint handles ATM-specific transactions by setting up the correct
     * source and destination accounts based on the transaction type. It validates
     * the account, ensures the ATM user exists, and delegates transaction creation.
     * </p>
     *
     * @param body the transaction details (fromAccount, amount, type, etc.)
     * @return ResponseEntity with the result of the transaction creation
     */
    @PostMapping("/atm")
    public ResponseEntity<?> processAtmTransaction(@RequestBody TransactionDto body) {
        // Retrieve the account by IBAN from the request
        Account fromAccount = accountService.getAccountByIban(body.getFromAccount());

        // If the account does not exist, return 404
        if (fromAccount == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account not found");
        }

        // Set both from and to account to the same account for ATM transactions
        // For ATM transactions, set both from and to account to the same IBAN initially
        body.setToAccount(body.getFromAccount());

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
        if (body.getTransactionType().equals(Transaction.TransactionType.DEPOSIT)) {
            // For deposits, money comes from the ATM user to the account
            body.setFromAccount(executingUser.getAccounts().getFirst().getIban());
        } else if (body.getTransactionType().equals(Transaction.TransactionType.WITHDRAW)) {
            // For withdrawals, money goes from the account to the ATM user
            body.setToAccount(executingUser.getAccounts().getFirst().getIban());
        } else {
            // Invalid transaction type for ATM
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid transaction type for ATM transaction");
        }

        // Delegate to the general transaction creation logic
        return createTransaction(executingUser, body);
    }
}

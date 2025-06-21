package com.stefvisser.springyield.services;

import jakarta.transaction.Transactional;
import com.stefvisser.springyield.dto.TransactionDTO;
import com.stefvisser.springyield.dto.PaginatedDataDTO;
import com.stefvisser.springyield.models.Account;
import com.stefvisser.springyield.models.AccountType;
import com.stefvisser.springyield.models.Transaction;
import com.stefvisser.springyield.repositories.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementation of the TransactionService interface that provides functionality for managing bank transactions.
 * This service handles various transaction operations including:
 * - Creating new transactions between accounts
 * - Retrieving transaction information
 * - Processing ATM transactions (withdrawals and deposits)
 * - Validating transaction rules and constraints
 * <p>
 * The service ensures that all banking rules are enforced, such as:
 * - Sufficient balance checks
 * - Daily and absolute transaction limits
 * - Savings account transfer restrictions
 *
 * @see TransactionService
 * @see Transaction
 * @see Account
 */
@Service
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;

    /**
     * Constructs a new TransactionServiceImpl with the required dependencies.
     *
     * @param transactionRepository Repository for transaction data operations
     * @param accountService        Service for accessing and modifying account data
     */
    public TransactionServiceImpl(TransactionRepository transactionRepository, AccountService accountService) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
    }

    /**
     * Retrieves all transactions from the database.
     *
     * @return List of all transactions
     */
    @Override
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    /**
     * Retrieves a specific transaction by its identifier.
     *
     * @param id The unique identifier of the transaction
     * @return The transaction with the specified ID, or null if not found
     */
    @Override
    public Transaction getTransactionById(long id) {
        return transactionRepository.findById(id).orElse(null);
    }

    /**
     * Retrieves all transactions that match a specific reference.
     *
     * @param reference The reference string to search for
     * @return List of transactions matching the reference
     */
    @Override
    public List<Transaction> getTransactionsByReference(String reference) {
        return transactionRepository.findByReference(reference);
    }

    /**
     * Retrieves all transactions related to a specific IBAN (International Bank Account Number).
     * This includes transactions where the IBAN is either the source or destination account.
     *
     * @param iban The IBAN to search for
     * @return List of transactions involving the specified IBAN
     */
    @Override
    public List<Transaction> getTransactionsByIBAN(String iban) {
        return transactionRepository.findByFromAccountOrToAccount(iban, iban);
    }

    /**
     * Creates a new transaction between two accounts.
     * This method implements a complete transaction workflow including:
     * - Account validation
     * - Transaction parameter validation
     * - Balance verification
     * - Savings account transfer rules
     * - Transaction recording
     * - Account balance updates
     *
     * @param transaction The transaction data transfer object containing details of the transaction
     * @return The created transaction entity
     * @throws ResponseStatusException If any validation fails or an error occurs during transaction processing
     */
    @Override
    @Transactional
    public Transaction createTransaction(TransactionDTO transaction) {
        try {
            // Get and validate accounts
            AccountValidationResult accountResult = validateAccounts(transaction);
            Account fromAccount = accountResult.fromAccount;
            Account toAccount = accountResult.toAccount;

            // Validate savings account transfers
            validateSavingsAccountTransfers(fromAccount, toAccount);

            // Validate transaction parameters
            validateTransactionParameters(fromAccount, transaction.getTransferAmount());

            // Create and save transaction entity
            Transaction transactionEntity = createTransactionEntity(transaction);

            // Update account balances
            updateAccountBalances(fromAccount, toAccount, transaction.getTransferAmount());

            // Link transaction with accounts
            linkTransactionToAccounts(fromAccount, toAccount, transactionEntity);

            // Update accounts in database
            updateAccountsInDatabase(fromAccount, toAccount);

            return transactionEntity;

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error during transaction creation: " + e.getMessage());
        }
    }

    /**
     * Validates that savings accounts are only used for transfers between accounts of the same owner.
     *
     * @param fromAccount The source account
     * @param toAccount   The destination account
     * @throws ResponseStatusException if validation fails
     */
    private void validateSavingsAccountTransfers(Account fromAccount, Account toAccount) {
        boolean isFromSavings = fromAccount.getAccountType() == AccountType.SAVINGS;
        boolean isToSavings = toAccount.getAccountType() == AccountType.SAVINGS;

        // If either account is a savings account
        if (isFromSavings || isToSavings) {
            // Check if the accounts have different owners
            if (!fromAccount.getUser().getUserId().equals(toAccount.getUser().getUserId())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Savings accounts can only be used for transfers between accounts of the same owner"
                );
            }
        }
    }

    /**
     * A record class that encapsulates the results of account validation.
     * This serves as a convenient container for returning multiple values from the validateAccounts method.
     */
    private record AccountValidationResult(Account fromAccount, Account toAccount) {
    }

    /**
     * Validates the existence and validity of the accounts involved in a transaction.
     *
     * @param transaction The transaction DTO containing the account information
     * @return An AccountValidationResult containing the validated source and destination accounts
     * @throws ResponseStatusException If either account does not exist or is invalid
     */
    private AccountValidationResult validateAccounts(TransactionDTO transaction) {
        Account fromAccount = accountService.getAccountByIban(transaction.getFromAccount());
        Account toAccount = accountService.getAccountByIban(transaction.getToAccount());

        if (fromAccount == null || toAccount == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid account details");
        }

        return new AccountValidationResult(fromAccount, toAccount);
    }

    /**
     * Validates the transaction parameters against account limits and balance.
     * This method checks:
     * - That the transfer amount is positive
     * - That the transfer amount does not exceed the account's absolute limit
     * - That the account has sufficient balance for the transfer
     * - That the transfer amount does not exceed the daily limit (if applicable)
     *
     * @param fromAccount    The source account for the transaction
     * @param transferAmount The amount to be transferred
     * @throws ResponseStatusException If any validation check fails
     */
    private void validateTransactionParameters(Account fromAccount, BigDecimal transferAmount) {
        BigDecimal balance = fromAccount.getBalance();
        BigDecimal limit = fromAccount.getAbsoluteLimit();
        BigDecimal dailyLimit = fromAccount.getDailyLimit();

        // Validate transfer amount
        if (transferAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transfer amount must be greater than zero");
        }

        // Validate against account limit
        if (transferAmount.compareTo(limit) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transfer amount exceeds account limit");
        }

        // Validate sufficient balance
        if (balance.subtract(transferAmount).compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance for transfer");
        }

        // Validate daily limit if applicable
        if (fromAccount.getDailyLimit() != null && transferAmount.compareTo(dailyLimit) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transfer amount exceeds daily limit");
        }
    }

    /**
     * Creates and persists a Transaction entity from the provided DTO.
     * Sets the current timestamp on the transaction.
     *
     * @param transaction The transaction DTO containing the transaction details
     * @return The created and persisted Transaction entity
     * @throws ResponseStatusException If an error occurs while saving the transaction
     */
    private Transaction createTransactionEntity(TransactionDTO transaction) {
        Transaction transactionEntity = Transaction.fromDTO(transaction);
        transactionEntity.setTimestamp(LocalDateTime.now());

        transactionEntity.setReference(createUniqueReference(transactionEntity)); // Generate a unique reference

        try {
            return transactionRepository.save(transactionEntity);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error saving transaction: " + e.getMessage());
        }
    }

    /**
     * Generates a unique reference for a transaction.
     * The reference is based on the current timestamp and the last six digits of the transaction ID.
     * If the transaction ID is not available, it generates a random six-digit number.
     *
     * @param transaction The transaction for which to create a unique reference
     * @return A unique reference string for the transaction
     */
    private String createUniqueReference(Transaction transaction) {
        int lastSixDigits = (transaction.getTransactionId() != null) ? (int) (transaction.getTransactionId() % 1000000) : (int) (Math.random() * 1000000);
        return String.format("TR%d%06d", System.currentTimeMillis(), lastSixDigits);
    }

    /**
     * Updates the balances of the accounts involved in a transaction.
     * Subtracts the transfer amount from the source account and adds it to the destination account.
     *
     * @param fromAccount    The source account
     * @param toAccount      The destination account
     * @param transferAmount The amount being transferred
     */
    private void updateAccountBalances(Account fromAccount, Account toAccount, BigDecimal transferAmount) {
        BigDecimal newFromBalance = fromAccount.getBalance().subtract(transferAmount);
        BigDecimal newToBalance = toAccount.getBalance().add(transferAmount);

        fromAccount.setBalance(newFromBalance);
        toAccount.setBalance(newToBalance);
    }

    /**
     * Links a transaction to its associated accounts.
     * Adds the transaction to the transaction lists of both the source and destination accounts.
     *
     * @param fromAccount       The source account
     * @param toAccount         The destination account
     * @param transactionEntity The transaction entity to be linked
     * @throws ResponseStatusException If the transaction lists are not initialized or if an error occurs
     */
    private void linkTransactionToAccounts(Account fromAccount, Account toAccount, Transaction transactionEntity) {
        if (fromAccount.getTransactions() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Source account transaction list is not initialized");
        }

        if (toAccount.getTransactions() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Destination account transaction list is not initialized");
        }

        try {
            fromAccount.getTransactions().add(transactionEntity);
            toAccount.getTransactions().add(transactionEntity);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error adding transaction to accounts: " + e.getMessage());
        }
    }

    /**
     * Persists the updated account information to the database.
     * This method is called after account balances have been updated for a transaction.
     *
     * @param fromAccount The source account with updated balance
     * @param toAccount   The destination account with updated balance
     * @throws ResponseStatusException If an error occurs while updating the accounts
     */
    private void updateAccountsInDatabase(Account fromAccount, Account toAccount) {
        try {
            accountService.updateAccount(fromAccount);
            accountService.updateAccount(toAccount);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error updating accounts: " + e.getMessage());
        }
    }

    /**
     * Searches for transactions based on various criteria such as query, type, date range, and amount.
     * This method supports pagination through limit and offset parameters.
     *
     * @param query          The search query string
     * @param type           The type of transaction to filter by (e.g., deposit, withdrawal)
     * @param limit          The maximum number of results to return
     * @param offset         The starting point for pagination
     * @param startDate      The start date for filtering transactions
     * @param endDate        The end date for filtering transactions
     * @param amountFrom     The minimum amount for filtering transactions
     * @param amountTo       The maximum amount for filtering transactions
     * @param amountOperator The operator to use for amount filtering (e.g., ">", "<", "=")
     * @return A PaginatedDataDTO containing the search results and pagination information
     */
    @Override
    public PaginatedDataDTO<TransactionDTO> searchTransactions(
            String query,
            String type,
            int limit,
            int offset,
            LocalDateTime startDate,
            LocalDateTime endDate,
            BigDecimal amountFrom,
            BigDecimal amountTo,
            String amountOperator
    ) {
        return transactionRepository.searchTransactions(
                query,
                type,
                startDate,
                endDate,
                amountFrom,
                amountTo,
                amountOperator,
                limit,
                offset
        );
    }

    @Override
    public Transaction processAtmTransaction(TransactionDTO transactionDTO) {
        Account account = accountService.getAccountByIban(transactionDTO.getFromAccount());

        if (transactionDTO.getTransactionType() == Transaction.TransactionType.WITHDRAW) {
            if (account.getBalance().compareTo(transactionDTO.getTransferAmount()) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance for withdrawal");
            }
            account.setBalance(account.getBalance().subtract(transactionDTO.getTransferAmount()));
        } else {
            account.setBalance(account.getBalance().add(transactionDTO.getTransferAmount()));
        }

        Transaction transaction = Transaction.fromDTO(transactionDTO);
        transaction.setTimestamp(LocalDateTime.now());

        accountService.updateAccount(account);
        return transactionRepository.save(transaction);
    }
}


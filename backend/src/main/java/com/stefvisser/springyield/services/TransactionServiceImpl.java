package com.stefvisser.springyield.services;

import com.stefvisser.springyield.models.*;
import jakarta.transaction.Transactional;
import com.stefvisser.springyield.dto.TransactionRequestDto;
import com.stefvisser.springyield.dto.PaginatedDataDto;
import com.stefvisser.springyield.repositories.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final UserService userService;

    public TransactionServiceImpl(TransactionRepository transactionRepository, AccountService accountService, UserService userService) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.userService = userService;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // API Methods
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Searches for transactions based on various criteria with pagination.
     * This method is intended for use by employees who have permission to search transactions.
     *
     * @param execUser       The user executing the request (should be an authenticated employee).
     * @param query          The search query string to filter transactions.
     * @param type           The type of transaction to filter by (e.g., "DEPOSIT", "WITHDRAW").
     * @param limit          The maximum number of results to return per page (default is 10).
     * @param offset         The starting position for pagination (default is 0).
     * @param startDate      The start date for filtering transactions.
     * @param endDate        The end date for filtering transactions.
     * @param amountFrom     The minimum amount for filtering transactions.
     * @param amountTo       The maximum amount for filtering transactions.
     * @param amountOperator The operator to use for filtering amounts (e.g., ">", "<", "=").
     * @return A PaginatedDataDto containing the search results and pagination information.
     */
    public PaginatedDataDto<TransactionRequestDto> searchTransactions(
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
    ) {
        if (execUser == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");

        if (!execUser.isEmployee())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to search transactions");

        if (limit <= 0) limit = 10;
        if (offset < 0) offset = 0;
        if (query == null) query = "";

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

    /**
     * Retrieves a transaction by its ID.
     * This method is intended for use by employees who have permission to view transactions.
     *
     * @param execUser The user executing the request (should be an authenticated employee).
     * @param targetId The ID of the transaction to retrieve.
     * @return The Transaction object associated with the specified ID.
     */
    public Transaction getTransactionById(User execUser, long targetId) {
        if (execUser == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");

        if (!execUser.isEmployee())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to view this transaction");

        Transaction transaction = transactionRepository.findById(targetId).orElse(null);
        if (transaction == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found with ID: " + targetId);

        return transaction;
    }

    /**
     * Retrieves transactions by IBAN.
     * This method is intended for use by both employees and account owners.
     *
     * @param execUser The user executing the request (should be an authenticated user).
     * @param iban     The IBAN of the account to search transactions for.
     * @return A list of transactions associated with the specified IBAN.
     */
    public List<Transaction> getTransactionsByIban(User execUser, String iban) {
        if (execUser == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");

        Account account = accountService.getAccountByIban(execUser, iban);
        if (account == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account with IBAN " + iban + " not found");

        // Check if the user is an employee or the owner of the account
        if (!execUser.isEmployee() && !account.getUser().getUserId().equals(execUser.getUserId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not have access to the specified account");

        return transactionRepository.findByFromAccountOrToAccount(iban, iban);
    }

    /**
     * Retrieves transactions by their reference number.
     * This method is intended for use by employees who have permission to search transactions by reference.
     *
     * @param execUser  The user executing the request (should be an authenticated employee).
     * @param reference The reference number to search for.
     * @return A list of transactions matching the reference number.
     */

    public List<Transaction> getTransactionsByReference(User execUser, String reference) {
        if (execUser == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");

        if (!execUser.isEmployee())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to search transactions by reference");

        return transactionRepository.findByReference(reference);
    }

    /**
     * Retrieves all transactions from the database.
     * This method is intended for use by employees who have permission to view all transactions.
     *
     * @param execUser The user executing the request (should be an authenticated employee).
     * @return A list of all transactions.
     */

    public List<Transaction> getAllTransactions(User execUser) {
        if (execUser == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");

        return transactionRepository.findAll();
    }

    /**
     * Creates a transaction between two accounts based on the provided request DTO.
     * The transaction is processed with the authenticated user as the account owner.
     *
     * @param execUser          The user executing the transaction (should be an authenticated user).
     * @param transactionReqDto The DTO containing transaction details.
     * @return The created Transaction object.
     */

    @Transactional
    public Transaction createTransaction(User execUser, TransactionRequestDto transactionReqDto) {
        if (execUser == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");

        // Net zoals @Daan 4 uur werk om deze enkele regel toe te moeten voegen.
        // User user werkt niet want een authenticatedPrincipal User is niet een user entity...
        User accountOwner = userService.getUserById(execUser, execUser.getUserId());

        if (transactionReqDto.getFromAccount() == null || transactionReqDto.getToAccount() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both from account and to account must be provided");

        // Validate the IBANs of the accounts, before retrieving the actual Account objects
        validateAccounts(accountOwner, transactionReqDto);
        Account fromAccount = accountService.getAccountByIban(accountOwner, transactionReqDto.getFromAccount());
        Account toAccount = accountService.getAccountByIban(accountOwner, transactionReqDto.getToAccount());

        // Validate the transfer between accounts, before creating and saving the transaction
        validateTransfer(fromAccount, toAccount, transactionReqDto.getTransferAmount());
        Transaction transaction = createAndSaveTransaction(transactionReqDto);

        // Update account balances, assign the transaction to the accounts, and update the accounts in the database
        updateToAndFromAccount(fromAccount, toAccount, transaction);

        return transaction;
    }

    /**
     * Creates a transaction for ATM operations, which can be either a deposit or a withdrawal.
     * The transaction is processed with the ATM user as the counterparty.
     *
     * @param execUser          The user executing the transaction (should be an ATM user).
     * @param transactionReqDTO The DTO containing transaction details.
     * @return The created Transaction object.
     */

    public Transaction createAtmTransaction(User execUser, TransactionRequestDto transactionReqDTO) {
        // Retrieve the account by IBAN from the request
        Account fromAccount = accountService.getAccountByIban(execUser, transactionReqDTO.getFromAccount());

        if (fromAccount == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found with IBAN: " + transactionReqDTO.getFromAccount());

        // Set both from and to account to the same account for ATM transactions
        // For ATM transactions, set both from and to the same IBAN initially
        transactionReqDTO.setToAccount(transactionReqDTO.getFromAccount());

        // Ensure the account is associated with a user
        if (fromAccount.getUser() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account does not have an associated user");
        }

        // Retrieve the special ATM user by email
        User atmUser = userService.findByEmail("atms@springyield.com");

        if (atmUser == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ATM user not found");

        // Adjust the from/to account based on transaction type
        if (transactionReqDTO.getTransactionType().equals(TransactionType.DEPOSIT)) {
            // For deposits, money comes from the ATM user to the account
            transactionReqDTO.setFromAccount(atmUser.getAccounts().getFirst().getIban());
        } else if (transactionReqDTO.getTransactionType().equals(TransactionType.WITHDRAW)) {
            // For withdrawals, money goes from the account to the ATM user
            transactionReqDTO.setToAccount(atmUser.getAccounts().getFirst().getIban());
        } else {
            // Invalid transaction type for ATM
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid transaction type for ATM: " + transactionReqDTO.getTransactionType());
        }

        if (transactionReqDTO.getTransactionType() == TransactionType.WITHDRAW) {
            if (fromAccount.getBalance().compareTo(transactionReqDTO.getTransferAmount()) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance for withdrawal");
            }
            fromAccount.setBalance(fromAccount.getBalance().subtract(transactionReqDTO.getTransferAmount()));
        } else {
            fromAccount.setBalance(fromAccount.getBalance().add(transactionReqDTO.getTransferAmount()));
        }

        Transaction transaction = Transaction.fromDTO(transactionReqDTO);
        transaction.setTimestamp(LocalDateTime.now());

        accountService.updateAccount(fromAccount);
        return transactionRepository.save(transaction);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Non-API Methods (Less authentication required, since they are used internally)
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Saves a list of transactions to the database.
     *
     * @param transactions The list of transactions to be saved.
     */
    public void saveAll(List<Transaction> transactions) {
        transactionRepository.saveAll(transactions);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Transfer Validation and Processing
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Validates the accounts involved in the transaction.
     *
     * @param execUser          The user executing the transaction.
     * @param transactionReqDto The DTO containing transaction details.
     */
    private void validateAccounts(User execUser, TransactionRequestDto transactionReqDto) {
        // Get the temporary Account IBANs strings from the frontend request, and validate them
        String fromAccString = transactionReqDto.getFromAccount().replace(" ", "").toLowerCase();
        String toAccString = transactionReqDto.getToAccount().replace(" ", "").toLowerCase();

        if (fromAccString.equalsIgnoreCase(toAccString))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "From and To accounts cannot be the same");

        if (!execUser.hasAccount(fromAccString) && !execUser.isEmployee())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User does not have access to the specified accounts");
    }

    /**
     * Validates the transfer between two accounts based on various criteria.
     *
     * @param fromAccount    The account from which the funds are transferred.
     * @param toAccount      The account to which the funds are transferred.
     * @param transferAmount The amount of money to be transferred.
     */
    private void validateTransfer(Account fromAccount, Account toAccount, BigDecimal transferAmount) {
        BigDecimal balance = fromAccount.getBalance();
        BigDecimal absoluteLimit = fromAccount.getAbsoluteLimit();
        BigDecimal dailyLimit = fromAccount.getDailyLimit();

        // Validate accounts existence
        if (fromAccount == null || toAccount == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Accounts not found for the provided IBANs");

        // Check if the accounts have different owners
        if (fromAccount.getAccountType() == AccountType.SAVINGS || toAccount.getAccountType() == AccountType.SAVINGS) {
            if (!fromAccount.getUser().equals(toAccount.getUser())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Savings accounts can only be used for transfers between accounts of the same owner"
                );
            }
        }

        // Validate transfer amount
        if (transferAmount.compareTo(BigDecimal.ZERO) <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transfer amount must be greater than zero");

        // Validate against account limit
        if (transferAmount.compareTo(absoluteLimit) > 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transfer amount exceeds account limit");

        // Validate sufficient balance
        if (balance.subtract(transferAmount).compareTo(fromAccount.getBalanceLimit()) < 0) // balanceLimit is negative so the result will be negative
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance for transfer, cannot go below balance limit: " + fromAccount.getBalanceLimit());

        // Validate daily limit if applicable
        if (transferAmount.compareTo(dailyLimit) > 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transfer amount exceeds daily limit");

        // Validate if the transfer amount exceeds the absolute limit
        if (transferAmount.compareTo(fromAccount.getAbsoluteLimit()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transfer amount exceeds absolute limit");
        }
    }

    /**
     * Creates a new transaction from the provided DTO and saves it to the database.
     *
     * @param transactionReqDto The DTO containing transaction details.
     * @return The created Transaction object.
     */
    private Transaction createAndSaveTransaction(TransactionRequestDto transactionReqDto) {
        Transaction transaction = Transaction.fromDTO(transactionReqDto);
        transaction.setTimestamp(LocalDateTime.now());

        // Generate reference before saving
        transaction.setReference(String.format("TR%d%06d", System.currentTimeMillis(),
                (transaction.getTransactionId() != null) ?
                        (int) (transaction.getTransactionId() % 1000000) :
                        (int) (Math.random() * 1000000)));

        // Save the transaction to get an ID
        Transaction savedTransaction = transactionRepository.save(transaction);

        // Make sure the transaction has the correct ID from the saved entity
        transaction.setTransactionId(savedTransaction.getTransactionId());

        return transaction;
    }

    /**
     * Updates the balances of both accounts involved in the transaction and saves the transaction to both accounts.
     *
     * @param fromAccount The account from which the funds are transferred.
     * @param toAccount   The account to which the funds are transferred.
     * @param transaction The transaction being processed.
     */
    private void updateToAndFromAccount(Account fromAccount, Account toAccount, Transaction transaction) {
        // Update the balances of both accounts
        fromAccount.setBalance(fromAccount.getBalance().subtract(transaction.getTransferAmount()));
        toAccount.setBalance(toAccount.getBalance().add(transaction.getTransferAmount()));

        // Add the transaction to both accounts
        fromAccount.getTransactions().add(transaction);
        toAccount.getTransactions().add(transaction);

        // Update accounts in the database
        accountService.updateAccount(fromAccount);
        accountService.updateAccount(toAccount);
    }
}

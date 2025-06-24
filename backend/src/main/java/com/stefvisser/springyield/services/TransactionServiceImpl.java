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

    public TransactionServiceImpl(TransactionRepository transactionRepository, AccountService accountService) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // API Methods
    // -----------------------------------------------------------------------------------------------------------------
    @Override
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

    @Override
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

    @Override
    public List<Transaction> getTransactionsByIBAN(User execUser, String iban) {
        if (execUser == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");

        Account account = accountService.getAccountByIban(execUser, iban);
        if (account == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account with IBAN " + iban + " not found");

        // TODO: make more logically readable by making boolean or flipping the logic
        // Check if the user is an employee or the owner of the account
        if (!execUser.isEmployee() && !account.getUser().equals(execUser))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not have access to the specified account");

        return transactionRepository.findByFromAccountOrToAccount(iban, iban);
    }

    @Override
    public List<Transaction> getTransactionsByReference(User execUser, String reference) {
        if (execUser == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");

        if (!execUser.isEmployee())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to search transactions by reference");

        return transactionRepository.findByReference(reference);
    }

    @Override
    public List<Transaction> getAllTransactions(User execUser) {
        if (execUser == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");

        return transactionRepository.findAll();
    }

    @Override
    @Transactional
    public Transaction createTransaction(User execUser, TransactionRequestDto transactionReqDto) {
        if (execUser == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");

        if (transactionReqDto.getFromAccString() == null || transactionReqDto.getToAccString() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both from account and to account must be provided");

        // Get the temporary Account IBANs strings from the frontend request, and validate them
        String fromAccString = transactionReqDto.getFromAccString().replace(" ", "").toLowerCase();
        String toAccString = transactionReqDto.getToAccString().replace(" ", "").toLowerCase();

        if (fromAccString.equalsIgnoreCase(toAccString))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "From and To accounts cannot be the same");

        if (!execUser.hasAccount(fromAccString) && !execUser.isEmployee())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User does not have access to the specified accounts");

        // After validation the accounts, retrieve the actual Account objects
        Account fromAccount = accountService.getAccountByIban(execUser, transactionReqDto.getFromAccString());
        Account toAccount = accountService.getAccountByIban(execUser, transactionReqDto.getToAccString());

        if (fromAccount == null || toAccount == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Accounts not found for the provided IBANs");

        // Validate the transfer between accounts, before
        validateTransfer(fromAccount, toAccount, transactionReqDto.getTransferAmount());

        // Set time and unique reference, before creating the transaction in the database
        Transaction transaction = createAndSaveTransaction(transactionReqDto);

        // Update account balances, assign the transaction to the accounts, and update the accounts in the database
        updateToAndFromAccount(fromAccount, toAccount, transaction);

        return transaction;
    }

    @Override
    public Transaction processAtmTransaction(User execUser, TransactionRequestDto transactionReqDTO) {
        Account account = accountService.getAccountByIban(execUser, transactionReqDTO.getFromAccString());

        if (transactionReqDTO.getTransactionType() == TransactionType.WITHDRAW) {
            if (account.getBalance().compareTo(transactionReqDTO.getTransferAmount()) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance for withdrawal");
            }
            account.setBalance(account.getBalance().subtract(transactionReqDTO.getTransferAmount()));
        } else {
            account.setBalance(account.getBalance().add(transactionReqDTO.getTransferAmount()));
        }

        Transaction transaction = Transaction.fromDTO(transactionReqDTO);
        transaction.setTimestamp(LocalDateTime.now());

        accountService.updateAccount(account);
        return transactionRepository.save(transaction);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Non-API Methods (Less authentication required, since they are used internally)
    // -----------------------------------------------------------------------------------------------------------------

    @Override
    public void saveAll(List<Transaction> transactions) {
        transactionRepository.saveAll(transactions);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Transfer Validation and Processing
    // -----------------------------------------------------------------------------------------------------------------

    private void validateTransfer(Account fromAccount, Account toAccount, BigDecimal transferAmount) {
        BigDecimal balance = fromAccount.getBalance();
        BigDecimal limit = fromAccount.getAbsoluteLimit();
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
        if (transferAmount.compareTo(limit) > 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transfer amount exceeds account limit");

        // Validate sufficient balance
        if (balance.subtract(transferAmount).compareTo(BigDecimal.ZERO) < 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance for transfer");

        // Validate daily limit if applicable
        if (fromAccount.getDailyLimit() != null && transferAmount.compareTo(dailyLimit) > 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transfer amount exceeds daily limit");
    }

    private Transaction createAndSaveTransaction(TransactionRequestDto transactionReqDto) {
        Transaction transaction = Transaction.fromDTO(transactionReqDto);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setReference(String.format("TR%d%06d", System.currentTimeMillis(),
                (transaction.getTransactionId() != null) ?
                        (int) (transaction.getTransactionId() % 1000000) :
                        (int) (Math.random() * 1000000)));

        transactionRepository.save(transaction);

        return transaction;
    }

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
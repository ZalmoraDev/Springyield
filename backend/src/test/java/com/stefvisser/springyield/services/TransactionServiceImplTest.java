package com.stefvisser.springyield.services;

import com.stefvisser.springyield.dto.PaginatedDataDTO;
import com.stefvisser.springyield.dto.TransactionDTO;
import com.stefvisser.springyield.models.Account;
import com.stefvisser.springyield.models.Transaction;
import com.stefvisser.springyield.models.User;
import com.stefvisser.springyield.models.UserRole;
import com.stefvisser.springyield.repositories.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountService accountService; // Mock AccountService as it's a dependency

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private Transaction sampleTransaction;
    private Account sampleFromAccount;
    private Account sampleToAccount;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User("Test", "User", "password", "test@example.com", 123, "06", UserRole.APPROVED, new ArrayList<>());
        sampleUser.setUserId(1L);

        sampleFromAccount = new Account();
        sampleFromAccount.setIban("NL01INHO0000000001");
        sampleFromAccount.setUser(sampleUser);

        sampleToAccount = new Account();
        sampleToAccount.setIban("NL02INHO0000000002");
        sampleToAccount.setUser(sampleUser); // Or a different user

        sampleTransaction = new Transaction();
        sampleTransaction.setTransactionId(1L);
        sampleTransaction.setFromAccount(sampleFromAccount.getIban());
        sampleTransaction.setToAccount(sampleToAccount.getIban());
        sampleTransaction.setTransferAmount(BigDecimal.valueOf(100.00));
        sampleTransaction.setDescription("Test transaction");
        sampleTransaction.setTimestamp(LocalDateTime.now());
        sampleTransaction.setReference("REF123");
        sampleTransaction.setTransactionType(Transaction.TransactionType.TRANSFER);
    }

    @Test
    void getAllTransactions_returnsListOfTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(sampleTransaction);
        when(transactionRepository.findAll()).thenReturn(transactions);

        List<Transaction> result = transactionService.getAllTransactions();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("REF123", result.get(0).getReference());
        verify(transactionRepository).findAll();
    }

    @Test
    void getTransactionById_whenTransactionExists_returnsTransaction() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(sampleTransaction));

        Transaction result = transactionService.getTransactionById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getTransactionId());
        verify(transactionRepository).findById(1L);
    }

    @Test
    void getTransactionById_whenTransactionDoesNotExist_returnsNull() {
        when(transactionRepository.findById(anyLong())).thenReturn(Optional.empty());

        Transaction result = transactionService.getTransactionById(99L);

        assertNull(result);
        verify(transactionRepository).findById(99L);
    }

    @Test
    void getTransactionsByReference_returnsListOfTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(sampleTransaction);
        when(transactionRepository.findByReference("REF123")).thenReturn(transactions);

        List<Transaction> result = transactionService.getTransactionsByReference("REF123");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("REF123", result.get(0).getReference());
        verify(transactionRepository).findByReference("REF123");
    }

    @Test
    void getTransactionsByIBAN_returnsListOfTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(sampleTransaction);
        String iban = "NL01INHO0000000001";
        when(transactionRepository.findByFromAccountOrToAccount(iban, iban)).thenReturn(transactions);

        List<Transaction> result = transactionService.getTransactionsByIBAN(iban);

        assertNotNull(result);
        assertEquals(1, result.size());
        // Further checks can be added to ensure the transaction involves the IBAN
        verify(transactionRepository).findByFromAccountOrToAccount(iban, iban);
    }

    @Test
    void searchTransactions_callsRepositorySearch() {
        // Arrange
        String query = "test";
        String type = "TRANSFER";
        int limit = 10;
        int offset = 0;
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        BigDecimal amountFrom = null;
        BigDecimal amountTo = null;
        String amountOperator = "";

        PaginatedDataDTO<TransactionDTO> expectedResponse = new PaginatedDataDTO<>(new ArrayList<>(), 0);

        when(transactionRepository.searchTransactions(
                anyString(),    // query
                anyString(),    // type
                any(),         // startDate
                any(),         // endDate
                any(),         // amountFrom
                any(),         // amountTo
                anyString(),   // amountOperator
                anyInt(),      // limit
                anyInt()       // offset
        )).thenReturn(expectedResponse);

        // Act
        PaginatedDataDTO<TransactionDTO> result = transactionService.searchTransactions(
                query, type, limit, offset,
                startDate, endDate,
                amountFrom, amountTo, amountOperator
        );

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(transactionRepository).searchTransactions(
                query, type, startDate, endDate,
                amountFrom, amountTo, amountOperator,
                limit, offset
        );
    }
}


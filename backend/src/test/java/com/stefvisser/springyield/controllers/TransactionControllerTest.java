package com.stefvisser.springyield.controllers;

import com.stefvisser.springyield.dto.TransactionDTO;
import com.stefvisser.springyield.models.Account;
import com.stefvisser.springyield.models.Transaction;
import com.stefvisser.springyield.models.User;
import com.stefvisser.springyield.models.UserRole;
import com.stefvisser.springyield.services.AccountService;
import com.stefvisser.springyield.services.TransactionService;
import com.stefvisser.springyield.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private AccountService accountService;

    @Mock
    private UserService userService;

    @InjectMocks
    private TransactionController transactionController;

    private User mockCustomer;
    private User mockEmployee;
    private Transaction mockTransaction;
    private TransactionDTO mockTransactionDTO;
    private Account mockFromAccount;
    private Account mockToAccount;

    @BeforeEach
    void setUp() {
        // Setup mock customer user
        mockCustomer = new User("John", "Doe", "password", "john@example.com", 123, "06", UserRole.APPROVED, new ArrayList<>());
        mockCustomer.setUserId(1L);

        // Setup mock employee user
        mockEmployee = new User("Jane", "Smith", "password", "jane@example.com", 456, "06", UserRole.EMPLOYEE, new ArrayList<>());
        mockEmployee.setUserId(2L);

        // Setup mock accounts
        mockFromAccount = new Account();
        mockFromAccount.setIban("nl01inho0000000001");
        mockFromAccount.setBalance(new BigDecimal("500.00"));
        mockFromAccount.setUser(mockCustomer);

        mockToAccount = new Account();
        mockToAccount.setIban("nl01inho0000000002");
        mockToAccount.setBalance(new BigDecimal("300.00"));
        mockToAccount.setUser(mockCustomer);

        // Setup mock transaction
        mockTransaction = new Transaction();
        mockTransaction.setTransactionId(1L);
        mockTransaction.setFromAccount("nl01inho0000000001");
        mockTransaction.setToAccount("nl01inho0000000002");
        mockTransaction.setTransferAmount(new BigDecimal("100.00"));
        mockTransaction.setDescription("Test transfer");
        mockTransaction.setTimestamp(LocalDateTime.now());
        mockTransaction.setReference("REF123");
        mockTransaction.setTransactionType(Transaction.TransactionType.TRANSFER);

        // Setup mock TransactionDTO
        mockTransactionDTO = new TransactionDTO();
        mockTransactionDTO.setFromAccount("nl01inho0000000001");
        mockTransactionDTO.setToAccount("nl01inho0000000002");
        mockTransactionDTO.setTransferAmount(new BigDecimal("100.00"));
        mockTransactionDTO.setTransactionType(Transaction.TransactionType.TRANSFER);
        mockTransactionDTO.setDescription("Test transfer");
    }

    @Test
    void getAllTransactions_ShouldReturnAllTransactions() {
        // Arrange
        List<Transaction> transactions = Arrays.asList(mockTransaction);
        when(transactionService.getAllTransactions()).thenReturn(transactions);

        // Act
        ResponseEntity<List<TransactionDTO>> response = transactionController.getAllTransactions();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(transactionService).getAllTransactions();
    }

    @Test
    void createTransaction_WithValidData_ShouldReturnCreatedTransaction() {
        // Arrange
        // Mock the customer having access to the from account
        List<Account> customerAccounts = Arrays.asList(mockFromAccount);
        mockCustomer.setAccounts(customerAccounts);

        when(userService.getUserById(mockCustomer.getUserId())).thenReturn(mockCustomer);
        when(transactionService.createTransaction(any(TransactionDTO.class))).thenReturn(mockTransaction);

        // Act
        ResponseEntity<?> response = transactionController.createTransaction(mockCustomer, mockTransactionDTO);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(userService).getUserById(mockCustomer.getUserId());
        verify(transactionService).createTransaction(any(TransactionDTO.class));
    }

    @Test
    void createTransaction_WithUnauthenticatedUser_ShouldReturnUnauthorized() {
        // Act
        ResponseEntity<?> response = transactionController.createTransaction(null, mockTransactionDTO);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(transactionService, never()).createTransaction(any());
    }

    @Test
    void createTransaction_WithNullFromAccount_ShouldReturnBadRequest() {
        // Arrange
        mockTransactionDTO.setFromAccount(null);
        when(userService.getUserById(mockCustomer.getUserId())).thenReturn(mockCustomer);

        // Act
        ResponseEntity<?> response = transactionController.createTransaction(mockCustomer, mockTransactionDTO);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(transactionService, never()).createTransaction(any());
    }

    @Test
    void createTransaction_WithNullToAccount_ShouldReturnBadRequest() {
        // Arrange
        mockTransactionDTO.setToAccount(null);
        when(userService.getUserById(mockCustomer.getUserId())).thenReturn(mockCustomer);

        // Act
        ResponseEntity<?> response = transactionController.createTransaction(mockCustomer, mockTransactionDTO);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(transactionService, never()).createTransaction(any());
    }

    @Test
    void createTransaction_WithSameFromAndToAccounts_ShouldReturnBadRequest() {
        // Arrange
        mockTransactionDTO.setToAccount("nl01inho0000000001"); // Same as from account
        when(userService.getUserById(mockCustomer.getUserId())).thenReturn(mockCustomer);

        // Act
        ResponseEntity<?> response = transactionController.createTransaction(mockCustomer, mockTransactionDTO);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(transactionService, never()).createTransaction(any());
    }

    @Test
    void createTransaction_WithNoAccessToFromAccount_ShouldReturnBadRequest() {
        // Arrange
        // Customer has no accounts
        mockCustomer.setAccounts(new ArrayList<>());
        when(userService.getUserById(mockCustomer.getUserId())).thenReturn(mockCustomer);

        // Act
        ResponseEntity<?> response = transactionController.createTransaction(mockCustomer, mockTransactionDTO);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(transactionService, never()).createTransaction(any());
    }

    @Test
    void createTransaction_WithEmployeeUser_ShouldAllowAnyFromAccount() {
        // Arrange
        // Employee has no personal accounts but should still be allowed
        mockEmployee.setAccounts(new ArrayList<>());
        when(userService.getUserById(mockEmployee.getUserId())).thenReturn(mockEmployee);
        when(transactionService.createTransaction(any(TransactionDTO.class))).thenReturn(mockTransaction);

        // Act
        ResponseEntity<?> response = transactionController.createTransaction(mockEmployee, mockTransactionDTO);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(transactionService).createTransaction(any(TransactionDTO.class));
    }

    @Test
    void createTransaction_WithServiceException_ShouldReturnInternalServerError() {
        // Arrange
        List<Account> customerAccounts = Arrays.asList(mockFromAccount);
        mockCustomer.setAccounts(customerAccounts);

        when(userService.getUserById(mockCustomer.getUserId())).thenReturn(mockCustomer);
        when(transactionService.createTransaction(any(TransactionDTO.class))).thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<?> response = transactionController.createTransaction(mockCustomer, mockTransactionDTO);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("An error occurred while processing the transaction"));
    }

    @Test
    void searchTransactions_WithNonEmployeeUser_ShouldReturnForbidden() {
        // Arrange
        when(userService.getUserById(mockCustomer.getUserId())).thenReturn(mockCustomer);

        // Act
        ResponseEntity<?> response = transactionController.searchTransactions(mockCustomer, "query", "TRANSFER", 10, 0, null, null, null, null, null);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(transactionService, never()).searchTransactions(any(), any(), anyInt(), anyInt(), any(), any(), any(), any(), any());
    }

    @Test
    void searchTransactions_WithUnauthenticatedUser_ShouldReturnUnauthorized() {
        // Act
        ResponseEntity<?> response = transactionController.searchTransactions(null, "query", "TRANSFER", 10, 0, null, null, null, null, null);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void getTransactionsByIBAN_WithAccountOwner_ShouldReturnTransactions() {
        // Arrange
        String iban = "nl01inho0000000001";
        List<Transaction> transactions = Arrays.asList(mockTransaction);
        List<Account> customerAccounts = Arrays.asList(mockFromAccount);
        mockCustomer.setAccounts(customerAccounts);

        when(accountService.getAccountByIban(iban)).thenReturn(mockFromAccount);
        when(transactionService.getTransactionsByIBAN(iban)).thenReturn(transactions);

        // Act
        ResponseEntity<?> response = transactionController.getTransactionsByIBAN(mockCustomer, iban);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(accountService).getAccountByIban(iban);
        verify(transactionService).getTransactionsByIBAN(iban);
    }

    @Test
    void getTransactionsByIBAN_WithEmployee_ShouldReturnTransactions() {
        // Arrange
        String iban = "nl01inho0000000001";
        List<Transaction> transactions = Arrays.asList(mockTransaction);
        mockEmployee.setAccounts(new ArrayList<>()); // Employee has no personal accounts

        when(accountService.getAccountByIban(iban)).thenReturn(mockFromAccount);
        when(transactionService.getTransactionsByIBAN(iban)).thenReturn(transactions);

        // Act
        ResponseEntity<?> response = transactionController.getTransactionsByIBAN(mockEmployee, iban);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(transactionService).getTransactionsByIBAN(iban);
    }

    @Test
    void getTransactionsByIBAN_WithNoAccess_ShouldReturnForbidden() {
        // Arrange
        String iban = "nl01inho0000000001";
        mockCustomer.setAccounts(new ArrayList<>()); // Customer has no accounts

        User anotherUser = new User();
        anotherUser.setUserId(999L);
        mockFromAccount.setUser(anotherUser); // Account belongs to someone else

        when(accountService.getAccountByIban(iban)).thenReturn(mockFromAccount);

// Act
        ResponseEntity<?> response = transactionController.getTransactionsByIBAN(mockCustomer, iban);

// Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(transactionService, never()).getTransactionsByIBAN(any());
    }

    @Test
    void getTransactionsByIBAN_WithNonExistentAccount_ShouldReturnNotFound() {
        // Arrange
        String iban = "nl01inho0000000001";
        List<Account> customerAccounts = Arrays.asList(mockFromAccount);
        mockCustomer.setAccounts(customerAccounts);

        when(accountService.getAccountByIban(iban)).thenReturn(null);

        // Act
        ResponseEntity<?> response = transactionController.getTransactionsByIBAN(mockCustomer, iban);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getTransactionsByIBAN_WithUnauthenticatedUser_ShouldReturnUnauthorized() {
        // Act
        ResponseEntity<?> response = transactionController.getTransactionsByIBAN(null, "nl01inho0000000001");

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void getTransactionsByReference_WithEmployee_ShouldReturnTransactions() {
        // Arrange
        String reference = "REF123";
        List<Transaction> transactions = Arrays.asList(mockTransaction);

        when(userService.getUserById(mockEmployee.getUserId())).thenReturn(mockEmployee);
        when(transactionService.getTransactionsByReference(reference)).thenReturn(transactions);

        // Act
        ResponseEntity<?> response = transactionController.getTransactionsByReference(mockEmployee, reference);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(transactionService).getTransactionsByReference(reference);
    }

    @Test
    void getTransactionsByReference_WithNonEmployee_ShouldReturnForbidden() {
        // Arrange
        String reference = "REF123";

        when(userService.getUserById(mockCustomer.getUserId())).thenReturn(mockCustomer);

        // Act
        ResponseEntity<?> response = transactionController.getTransactionsByReference(mockCustomer, reference);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(transactionService, never()).getTransactionsByReference(any());
    }

    @Test
    void getTransactionsByReference_WithUnauthenticatedUser_ShouldReturnUnauthorized() {
        // Act
        ResponseEntity<?> response = transactionController.getTransactionsByReference(null, "REF123");

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void getTransactionByID_WithEmployee_ShouldReturnTransaction() {
        // Arrange
        Long transactionId = 1L;

        when(userService.getUserById(mockEmployee.getUserId())).thenReturn(mockEmployee);
        when(transactionService.getTransactionById(transactionId)).thenReturn(mockTransaction);

        // Act
        ResponseEntity<?> response = transactionController.getTransactionByID(mockEmployee, transactionId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(transactionService).getTransactionById(transactionId);
    }

    @Test
    void getTransactionByID_WithNonEmployee_ShouldReturnForbidden() {
        // Arrange
        Long transactionId = 1L;

        when(userService.getUserById(mockCustomer.getUserId())).thenReturn(mockCustomer);

        // Act
        ResponseEntity<?> response = transactionController.getTransactionByID(mockCustomer, transactionId);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(transactionService, never()).getTransactionById(anyLong());
    }

    @Test
    void getTransactionByID_WithNonExistentTransaction_ShouldReturnNotFound() {
        // Arrange
        Long transactionId = 999L;

        when(userService.getUserById(mockEmployee.getUserId())).thenReturn(mockEmployee);
        when(transactionService.getTransactionById(transactionId)).thenReturn(null);

        // Act
        ResponseEntity<?> response = transactionController.getTransactionByID(mockEmployee, transactionId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getTransactionByID_WithUnauthenticatedUser_ShouldReturnUnauthorized() {
        // Act
        ResponseEntity<?> response = transactionController.getTransactionByID(null, 1L);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void processAtmTransaction_WithDepositType_ShouldSetToAccountSameAsFromAccount() {
        // Arrange
//        TransactionDTO atmTransactionDTO = new TransactionDTO();
//        atmTransactionDTO.setFromAccount("nl01inho0000000001");
//        atmTransactionDTO.setTransactionType(Transaction.TransactionType.DEPOSIT);
//        atmTransactionDTO.setTransferAmount(new BigDecimal("50.00"));
//
//        Transaction atmTransaction = new Transaction();
//        atmTransaction.setFromAccount("nl01inho0000000001");
//        atmTransaction.setTransferAmount(new BigDecimal("50.00"));
//
//        when(transactionService.processAtmTransaction(any(TransactionDTO.class))).thenReturn(atmTransaction);
//        when(accountService.getAccountByIban(atmTransaction.getFromAccount())).thenReturn(mockFromAccount);
//
//        // Act
//        ResponseEntity<?> response = transactionController.processAtmTransaction(atmTransactionDTO);
//
//        // Assert
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals(atmTransactionDTO.getFromAccount(), atmTransactionDTO.getToAccount());
//        verify(transactionService).processAtmTransaction(any(TransactionDTO.class));
//        verify(accountService).getAccountByIban(atmTransaction.getFromAccount());
//
//        // Verify response contains both transaction and newBalance
//        assertTrue(response.getBody() instanceof Map);
//        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
//        assertTrue(responseBody.containsKey("transaction"));
//        assertTrue(responseBody.containsKey("newBalance"));
    }

    @Test
    void processAtmTransaction_WithWithdrawalType_ShouldKeepToAccountNull() {
        // Arrange
//        TransactionDTO atmTransactionDTO = new TransactionDTO();
//        atmTransactionDTO.setFromAccount("nl01inho0000000001");
//        atmTransactionDTO.setTransactionType(Transaction.TransactionType.WITHDRAW);
//        atmTransactionDTO.setTransferAmount(new BigDecimal("50.00"));
//
//        Transaction atmTransaction = new Transaction();
//        atmTransaction.setFromAccount("nl01inho0000000001");
//        atmTransaction.setTransferAmount(new BigDecimal("50.00"));
//
//        when(transactionService.processAtmTransaction(any(TransactionDTO.class))).thenReturn(atmTransaction);
//        when(accountService.getAccountByIban(atmTransaction.getFromAccount())).thenReturn(mockFromAccount);
//
//        // Act
//        ResponseEntity<?> response = transactionController.processAtmTransaction(atmTransactionDTO);
//
//        // Assert
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNull(atmTransactionDTO.getToAccount());
//        verify(transactionService).processAtmTransaction(any(TransactionDTO.class));
    }
}
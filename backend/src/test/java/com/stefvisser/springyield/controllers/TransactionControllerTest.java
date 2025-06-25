package com.stefvisser.springyield.controllers;

import com.stefvisser.springyield.dto.PaginatedDataDto;
import com.stefvisser.springyield.dto.TransactionRequestDto;
import com.stefvisser.springyield.models.Transaction;
import com.stefvisser.springyield.models.TransactionType;
import com.stefvisser.springyield.models.User;
import com.stefvisser.springyield.models.UserRole;
import com.stefvisser.springyield.services.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionController transactionController;

    private User testEmployee;
    private User testCustomer;
    private Transaction testTransaction;
    private TransactionRequestDto testTransactionDto;
    private List<Transaction> transactionList;
    private PaginatedDataDto<TransactionRequestDto> paginatedData;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup test employee
        testEmployee = new User(
                "Employee",
                "Test",
                "password",
                "employee@example.com",
                123456789,
                "123-456-7890",
                UserRole.EMPLOYEE,
                new ArrayList<>()
        );
        testEmployee.setUserId(1L);

        // Setup test customer
        testCustomer = new User(
                "Customer",
                "Test",
                "password",
                "customer@example.com",
                987654321,
                "987-654-3210",
                UserRole.APPROVED,
                new ArrayList<>()
        );
        testCustomer.setUserId(2L);

        // Setup test transaction
        testTransaction = new Transaction();
        testTransaction.setTransactionId(1L);
        testTransaction.setTransactionType(TransactionType.TRANSFER);
        testTransaction.setFromAccount("NL91SPYD0000000001");
        testTransaction.setToAccount("NL91SPYD0000000002");
        testTransaction.setTransferAmount(new BigDecimal("100.00"));
        testTransaction.setReference("TR12345678901");
        testTransaction.setTimestamp(LocalDateTime.now());
        testTransaction.setDescription("Test transaction");

        // Setup transaction DTO
        testTransactionDto = TransactionRequestDto.wrap(testTransaction);

        // Setup transaction list
        transactionList = Arrays.asList(testTransaction);

        // Setup paginated data
        List<TransactionRequestDto> transactionDtoList = transactionList.stream()
                .map(TransactionRequestDto::wrap)
                .toList();
        paginatedData = new PaginatedDataDto<>(transactionDtoList, transactionDtoList.size());
    }

    @Test
    void searchTransactions_Success() {
        // Arrange
        String query = "test";
        String type = "TRANSFER";
        int limit = 10;
        int offset = 0;
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        BigDecimal amountFrom = new BigDecimal("50.00");
        BigDecimal amountTo = new BigDecimal("200.00");
        String amountOperator = ">";

        when(transactionService.searchTransactions(any(User.class), eq(query), eq(type), eq(limit), eq(offset),
                eq(startDate), eq(endDate), eq(amountFrom), eq(amountTo), eq(amountOperator)))
                .thenReturn(paginatedData);

        // Act
        ResponseEntity<?> response = transactionController.searchTransactions(testEmployee, query, type, limit, offset,
                startDate, endDate, amountFrom, amountTo, amountOperator);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof PaginatedDataDto);
        PaginatedDataDto<TransactionRequestDto> returnedData = (PaginatedDataDto<TransactionRequestDto>) response.getBody();
        assertEquals(1, returnedData.getTotalCount());
        assertEquals(1, returnedData.getData().size());
    }

    @Test
    void getTransactionById_Success() {
        // Arrange
        Long transactionId = 1L;
        when(transactionService.getTransactionById(any(User.class), eq(transactionId))).thenReturn(testTransaction);

        // Act
        ResponseEntity<?> response = transactionController.getTransactionById(testEmployee, transactionId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof TransactionRequestDto);
        TransactionRequestDto returnedDto = (TransactionRequestDto) response.getBody();
        assertEquals(testTransaction.getTransactionId(), returnedDto.getTransactionId());
    }

    @Test
    void getTransactionById_NotFound() {
        // Arrange
        Long transactionId = 999L;
        when(transactionService.getTransactionById(any(User.class), eq(transactionId)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found with ID: " + transactionId));

        // Act
        ResponseEntity<?> response = transactionController.getTransactionById(testEmployee, transactionId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getTransactionsByIban_Success() {
        // Arrange
        String iban = "NL91SPYD0000000001";
        when(transactionService.getTransactionsByIban(any(User.class), eq(iban))).thenReturn(transactionList);

        // Act
        ResponseEntity<?> response = transactionController.getTransactionsByIban(testCustomer, iban);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        List<TransactionRequestDto> returnedList = (List<TransactionRequestDto>) response.getBody();
        assertEquals(1, returnedList.size());
        assertEquals(testTransaction.getTransactionId(), returnedList.get(0).getTransactionId());
    }

    @Test
    void getTransactionsByReference_Success() {
        // Arrange
        String reference = "TR12345678901";
        when(transactionService.getTransactionsByReference(any(User.class), eq(reference))).thenReturn(transactionList);

        // Act
        ResponseEntity<?> response = transactionController.getTransactionsByReference(testEmployee, reference);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        List<TransactionRequestDto> returnedList = (List<TransactionRequestDto>) response.getBody();
        assertEquals(1, returnedList.size());
        assertEquals(testTransaction.getReference(), returnedList.get(0).getReference());
    }

    @Test
    void getAllTransactions_Success() {
        // Arrange
        when(transactionService.getAllTransactions(any(User.class))).thenReturn(transactionList);

        // Act
        ResponseEntity<List<TransactionRequestDto>> response = transactionController.getAllTransactions(testEmployee);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(testTransaction.getTransactionId(), response.getBody().get(0).getTransactionId());
    }

    @Test
    void createTransaction_Success() {
        // Arrange
        when(transactionService.createTransaction(any(User.class), any(TransactionRequestDto.class)))
                .thenReturn(testTransaction);

        // Act
        ResponseEntity<?> response = transactionController.createTransaction(testCustomer, testTransactionDto);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof TransactionRequestDto);
        TransactionRequestDto returnedDto = (TransactionRequestDto) response.getBody();
        assertEquals(testTransaction.getTransactionId(), returnedDto.getTransactionId());
    }

    @Test
    void createTransaction_InsufficientBalance() {
        // Arrange
        when(transactionService.createTransaction(any(User.class), any(TransactionRequestDto.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance for transfer"));

        // Act
        ResponseEntity<?> response = transactionController.createTransaction(testCustomer, testTransactionDto);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Insufficient balance for transfer", response.getBody());
    }

    @Test
    void createAtmTransaction_Success() {
        // Arrange
        TransactionRequestDto atmTransactionDto = new TransactionRequestDto();
        atmTransactionDto.setFromAccount("NL91SPYD0000000001");
        atmTransactionDto.setTransactionType(TransactionType.DEPOSIT);
        atmTransactionDto.setTransferAmount(new BigDecimal("50.00"));
        atmTransactionDto.setDescription("ATM deposit");

        when(transactionService.createAtmTransaction(any(User.class), any(TransactionRequestDto.class)))
                .thenReturn(testTransaction);

        // Act
        ResponseEntity<?> response = transactionController.createAtmTransaction(testCustomer, atmTransactionDto);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof TransactionRequestDto);
    }

    @Test
    void createAtmTransaction_InvalidTransactionType() {
        // Arrange
        TransactionRequestDto atmTransactionDto = new TransactionRequestDto();
        atmTransactionDto.setFromAccount("NL91SPYD0000000001");
        atmTransactionDto.setTransactionType(TransactionType.TRANSFER);
        atmTransactionDto.setTransferAmount(new BigDecimal("50.00"));
        atmTransactionDto.setDescription("Invalid ATM transaction");

        when(transactionService.createAtmTransaction(any(User.class), any(TransactionRequestDto.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid transaction type for ATM: " + TransactionType.TRANSFER));

        // Act
        ResponseEntity<?> response = transactionController.createAtmTransaction(testCustomer, atmTransactionDto);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid transaction type for ATM: TRANSFER", response.getBody());
    }
}

package com.stefvisser.springyield.services;

import com.stefvisser.springyield.dto.PaginatedDataDto;
import com.stefvisser.springyield.dto.TransactionRequestDto;
import com.stefvisser.springyield.models.*;
import com.stefvisser.springyield.repositories.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private UserService userService;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private User testEmployee;
    private User testCustomer;
    private User testAtmUser;
    private Account testFromAccount;
    private Account testToAccount;
    private Account testAtmAccount;
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

        // Setup test ATM user
        testAtmUser = new User(
                "ATM",
                "User",
                "password",
                "atms@springyield.com",
                111222333,
                "111-222-3333",
                UserRole.EMPLOYEE,
                new ArrayList<>()
        );
        testAtmUser.setUserId(3L);

        // Setup test from account
        testFromAccount = new Account(
                1L,
                testCustomer,
                "NL91SPYD0000000001",
                LocalDate.now(),
                AccountType.PAYMENT,
                new BigDecimal("1000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("2500.00"),
                new BigDecimal("-1000.00"),
                AccountStatus.ACTIVE,
                new ArrayList<>()
        );

        // Setup test to account
        testToAccount = new Account(
                2L,
                testCustomer,
                "NL91SPYD0000000002",
                LocalDate.now(),
                AccountType.SAVINGS,
                new BigDecimal("1000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("1500.00"),
                new BigDecimal("10000.00"),
                AccountStatus.ACTIVE,
                new ArrayList<>()
        );

        // Setup test ATM account
        testAtmAccount = new Account(
                3L,
                testAtmUser,
                "NL91SPYD0000000003",
                LocalDate.now(),
                AccountType.PAYMENT,
                new BigDecimal("1000000.00"),
                new BigDecimal("1000000.00"),
                new BigDecimal("1000000.00"),
                new BigDecimal("1000000.00"),
                AccountStatus.ACTIVE,
                new ArrayList<>()
        );

        // Add accounts to customer and ATM user
        testCustomer.getAccounts().add(testFromAccount);
        testCustomer.getAccounts().add(testToAccount);
        testAtmUser.getAccounts().add(testAtmAccount);

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
        testTransactionDto = new TransactionRequestDto();
        testTransactionDto.setTransactionId(1L);
        testTransactionDto.setTransactionType(TransactionType.TRANSFER);
        testTransactionDto.setFromAccount("NL91SPYD0000000001");
        testTransactionDto.setToAccount("NL91SPYD0000000002");
        testTransactionDto.setTransferAmount(new BigDecimal("100.00"));
        testTransactionDto.setDescription("Test transaction");

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

        when(transactionRepository.searchTransactions(
                eq(query), eq(type), eq(startDate), eq(endDate),
                eq(amountFrom), eq(amountTo), eq(amountOperator),
                eq(limit), eq(offset)))
                .thenReturn(paginatedData);

        // Act
        PaginatedDataDto<TransactionRequestDto> result = transactionService.searchTransactions(
                testEmployee, query, type, limit, offset, startDate, endDate, amountFrom, amountTo, amountOperator);

        // Assert
        assertNotNull(result);
        assertEquals(transactionList.size(), result.getTotalCount());
        verify(transactionRepository, times(1)).searchTransactions(
                eq(query), eq(type), eq(startDate), eq(endDate),
                eq(amountFrom), eq(amountTo), eq(amountOperator),
                eq(limit), eq(offset));
    }

    @Test
    void searchTransactions_DefaultParameters() {
        // Arrange
        when(transactionRepository.searchTransactions(
                eq(""), eq(null), eq(null), eq(null),
                eq(null), eq(null), eq(null),
                eq(10), eq(0)))
                .thenReturn(paginatedData);

        // Act
        PaginatedDataDto<TransactionRequestDto> result = transactionService.searchTransactions(
                testEmployee, null, null, 0, 0, null, null, null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(transactionList.size(), result.getTotalCount());
        verify(transactionRepository, times(1)).searchTransactions(
                eq(""), eq(null), eq(null), eq(null),
                eq(null), eq(null), eq(null),
                eq(10), eq(0));
    }

    @Test
    void searchTransactions_Unauthorized() {
        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> transactionService.searchTransactions(
                        null, "", null, 10, 0, null, null, null, null, null));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("User not authenticated", exception.getReason());
    }

    @Test
    void searchTransactions_Forbidden_NonEmployee() {
        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> transactionService.searchTransactions(
                        testCustomer, "", null, 10, 0, null, null, null, null, null));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("You do not have permission to search transactions", exception.getReason());
    }

    @Test
    void getTransactionById_Success() {
        // Arrange
        when(transactionRepository.findById(testTransaction.getTransactionId()))
                .thenReturn(Optional.of(testTransaction));

        // Act
        Transaction result = transactionService.getTransactionById(testEmployee, testTransaction.getTransactionId());

        // Assert
        assertNotNull(result);
        assertEquals(testTransaction.getTransactionId(), result.getTransactionId());
        verify(transactionRepository, times(1)).findById(testTransaction.getTransactionId());
    }

    @Test
    void getTransactionById_Unauthorized() {
        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> transactionService.getTransactionById(null, 1L));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("User not authenticated", exception.getReason());
    }

    @Test
    void getTransactionById_Forbidden_NonEmployee() {
        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> transactionService.getTransactionById(testCustomer, 1L));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("You do not have permission to view this transaction", exception.getReason());
    }

    @Test
    void getTransactionById_NotFound() {
        // Arrange
        when(transactionRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> transactionService.getTransactionById(testEmployee, 999L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Transaction not found"));
    }

    @Test
    void getTransactionsByIban_Success_Employee() {
        // Arrange
        String iban = testFromAccount.getIban();
        when(accountService.getAccountByIban(testEmployee, iban)).thenReturn(testFromAccount);
        when(transactionRepository.findByFromAccountOrToAccount(iban, iban)).thenReturn(transactionList);

        // Act
        List<Transaction> result = transactionService.getTransactionsByIban(testEmployee, iban);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testTransaction.getTransactionId(), result.get(0).getTransactionId());
        verify(accountService, times(1)).getAccountByIban(testEmployee, iban);
        verify(transactionRepository, times(1)).findByFromAccountOrToAccount(iban, iban);
    }

    @Test
    void getTransactionsByIban_Success_AccountOwner() {
        // Arrange
        String iban = testFromAccount.getIban();
        when(accountService.getAccountByIban(testCustomer, iban)).thenReturn(testFromAccount);
        when(transactionRepository.findByFromAccountOrToAccount(iban, iban)).thenReturn(transactionList);

        // Act
        List<Transaction> result = transactionService.getTransactionsByIban(testCustomer, iban);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testTransaction.getTransactionId(), result.get(0).getTransactionId());
    }

    @Test
    void getTransactionsByIban_Unauthorized() {
        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> transactionService.getTransactionsByIban(null, "NL91SPYD0000000001"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("User not authenticated", exception.getReason());
    }

    @Test
    void getTransactionsByIban_AccountNotFound() {
        // Arrange
        String iban = "NL91SPYD9999999999";
        when(accountService.getAccountByIban(any(User.class), eq(iban)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> transactionService.getTransactionsByIban(testEmployee, iban));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void getTransactionsByReference_Success() {
        // Arrange
        String reference = testTransaction.getReference();
        when(transactionRepository.findByReference(reference)).thenReturn(transactionList);

        // Act
        List<Transaction> result = transactionService.getTransactionsByReference(testEmployee, reference);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(reference, result.get(0).getReference());
        verify(transactionRepository, times(1)).findByReference(reference);
    }

    @Test
    void getTransactionsByReference_Unauthorized() {
        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> transactionService.getTransactionsByReference(null, "TR12345678901"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("User not authenticated", exception.getReason());
    }

    @Test
    void getTransactionsByReference_Forbidden_NonEmployee() {
        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> transactionService.getTransactionsByReference(testCustomer, "TR12345678901"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("You do not have permission to search transactions by reference", exception.getReason());
    }

    @Test
    void getAllTransactions_Success() {
        // Arrange
        when(transactionRepository.findAll()).thenReturn(transactionList);

        // Act
        List<Transaction> result = transactionService.getAllTransactions(testEmployee);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testTransaction.getTransactionId(), result.get(0).getTransactionId());
        verify(transactionRepository, times(1)).findAll();
    }

    @Test
    void getAllTransactions_Unauthorized() {
        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> transactionService.getAllTransactions(null));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("User not authenticated", exception.getReason());
    }

    @Test
    void createTransaction_Success() {
        // Arrange
        when(userService.getUserById(testCustomer, testCustomer.getUserId())).thenReturn(testCustomer);
        when(accountService.getAccountByIban(testCustomer, testTransactionDto.getFromAccount())).thenReturn(testFromAccount);
        when(accountService.getAccountByIban(testCustomer, testTransactionDto.getToAccount())).thenReturn(testToAccount);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // Act
        Transaction result = transactionService.createTransaction(testCustomer, testTransactionDto);

        // Assert
        assertNotNull(result);
        assertEquals(testTransaction.getTransactionId(), result.getTransactionId());
        verify(accountService, times(1)).updateAccount(testFromAccount);
        verify(accountService, times(1)).updateAccount(testToAccount);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void createTransaction_Unauthorized() {
        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> transactionService.createTransaction(null, testTransactionDto));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void createTransaction_MissingAccounts() {
        // Arrange
        TransactionRequestDto invalidDto = new TransactionRequestDto();
        invalidDto.setTransferAmount(new BigDecimal("100.00"));

        when(userService.getUserById(testCustomer, testCustomer.getUserId())).thenReturn(testCustomer);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> transactionService.createTransaction(testCustomer, invalidDto));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Both from account and to account must be provided", exception.getReason());
    }

    @Test
    void createTransaction_SameAccount() {
        // Arrange
        TransactionRequestDto sameAccountDto = new TransactionRequestDto();
        sameAccountDto.setFromAccount("NL91SPYD0000000001");
        sameAccountDto.setToAccount("NL91SPYD0000000001");
        sameAccountDto.setTransferAmount(new BigDecimal("100.00"));

        when(userService.getUserById(testCustomer, testCustomer.getUserId())).thenReturn(testCustomer);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> transactionService.createTransaction(testCustomer, sameAccountDto));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("From and To accounts cannot be the same", exception.getReason());
    }

    @Test
    void createTransaction_InsufficientBalance() {
        // Arrange
        TransactionRequestDto largeAmountDto = new TransactionRequestDto();
        largeAmountDto.setFromAccount("NL91SPYD0000000001");
        largeAmountDto.setToAccount("NL91SPYD0000000002");
        largeAmountDto.setTransferAmount(new BigDecimal("5000.00")); // More than balance

        when(userService.getUserById(testCustomer, testCustomer.getUserId())).thenReturn(testCustomer);
        when(accountService.getAccountByIban(testCustomer, largeAmountDto.getFromAccount())).thenReturn(testFromAccount);
        when(accountService.getAccountByIban(testCustomer, largeAmountDto.getToAccount())).thenReturn(testToAccount);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> transactionService.createTransaction(testCustomer, largeAmountDto));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Insufficient balance for transfer, cannot go below balance limit: -1000.00", exception.getReason());
    }

    @Test
    void createAtmTransaction_Deposit_Success() {
        // Arrange
        TransactionRequestDto depositDto = new TransactionRequestDto();
        depositDto.setFromAccount("NL91SPYD0000000001");
        depositDto.setTransactionType(TransactionType.DEPOSIT);
        depositDto.setTransferAmount(new BigDecimal("100.00"));

        when(accountService.getAccountByIban(testCustomer, depositDto.getFromAccount())).thenReturn(testFromAccount);
        when(userService.findByEmail("atms@springyield.com")).thenReturn(testAtmUser);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // Act
        Transaction result = transactionService.createAtmTransaction(testCustomer, depositDto);

        // Assert
        assertNotNull(result);
        verify(accountService, times(1)).updateAccount(testFromAccount);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void createAtmTransaction_Withdraw_Success() {
        // Arrange
        TransactionRequestDto withdrawDto = new TransactionRequestDto();
        withdrawDto.setFromAccount("NL91SPYD0000000001");
        withdrawDto.setTransactionType(TransactionType.WITHDRAW);
        withdrawDto.setTransferAmount(new BigDecimal("100.00"));

        when(accountService.getAccountByIban(testCustomer, withdrawDto.getFromAccount())).thenReturn(testFromAccount);
        when(userService.findByEmail("atms@springyield.com")).thenReturn(testAtmUser);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // Act
        Transaction result = transactionService.createAtmTransaction(testCustomer, withdrawDto);

        // Assert
        assertNotNull(result);
        verify(accountService, times(1)).updateAccount(testFromAccount);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void createAtmTransaction_Withdraw_InsufficientBalance() {
        // Arrange
        TransactionRequestDto withdrawDto = new TransactionRequestDto();
        withdrawDto.setFromAccount("NL91SPYD0000000001");
        withdrawDto.setTransactionType(TransactionType.WITHDRAW);
        withdrawDto.setTransferAmount(new BigDecimal("5000.00")); // More than balance

        Account lowBalanceAccount = new Account(
                1L,
                testCustomer,
                "NL91SPYD0000000001",
                LocalDate.now(),
                AccountType.PAYMENT,
                new BigDecimal("100.00"), // Low balance
                new BigDecimal("5000.00"),
                new BigDecimal("2500.00"),
                new BigDecimal("10000.00"),
                AccountStatus.ACTIVE,
                new ArrayList<>()
        );

        when(accountService.getAccountByIban(testCustomer, withdrawDto.getFromAccount())).thenReturn(lowBalanceAccount);
        when(userService.findByEmail("atms@springyield.com")).thenReturn(testAtmUser);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> transactionService.createAtmTransaction(testCustomer, withdrawDto));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Insufficient balance for withdrawal", exception.getReason());
    }

    @Test
    void createAtmTransaction_InvalidType() {
        // Arrange
        TransactionRequestDto invalidTypeDto = new TransactionRequestDto();
        invalidTypeDto.setFromAccount("NL91SPYD0000000001");
        invalidTypeDto.setTransactionType(TransactionType.TRANSFER);
        invalidTypeDto.setTransferAmount(new BigDecimal("100.00"));

        when(accountService.getAccountByIban(testCustomer, invalidTypeDto.getFromAccount())).thenReturn(testFromAccount);
        when(userService.findByEmail("atms@springyield.com")).thenReturn(testAtmUser);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> transactionService.createAtmTransaction(testCustomer, invalidTypeDto));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Invalid transaction type for ATM: TRANSFER", exception.getReason());
    }

    @Test
    void saveAll_Success() {
        // Arrange
        when(transactionRepository.saveAll(anyList())).thenReturn(transactionList);

        // Act
        transactionService.saveAll(transactionList);

        // Assert
        verify(transactionRepository, times(1)).saveAll(transactionList);
    }
}

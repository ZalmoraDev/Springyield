package com.stefvisser.springyield.services;

import com.stefvisser.springyield.dto.AccountProfileDto;
import com.stefvisser.springyield.dto.PaginatedDataDto;
import com.stefvisser.springyield.models.Account;
import com.stefvisser.springyield.models.AccountStatus;
import com.stefvisser.springyield.models.AccountType;
import com.stefvisser.springyield.models.User;
import com.stefvisser.springyield.models.UserRole;
import com.stefvisser.springyield.repositories.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    private User testEmployee;
    private User testCustomer;
    private Account testAccount;
    private Account testSavingsAccount;
    private List<Account> accountList;
    private PaginatedDataDto<AccountProfileDto> paginatedData;

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

        // Setup test payment account
        testAccount = new Account(
                1L,
                testCustomer,
                "NL91SPYD0000000001",
                LocalDate.now(),
                AccountType.PAYMENT,
                new BigDecimal("1000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("2500.00"),
                new BigDecimal("10000.00"),
                AccountStatus.ACTIVE,
                BigDecimal.ZERO,
                new ArrayList<>()
        );

        // Setup test savings account
        testSavingsAccount = new Account(
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
                BigDecimal.ZERO,
                new ArrayList<>()
        );

        // Add accounts to customer
        testCustomer.getAccounts().add(testAccount);
        testCustomer.getAccounts().add(testSavingsAccount);

        // Setup account list
        accountList = Arrays.asList(testAccount, testSavingsAccount);

        // Setup paginated data
        List<AccountProfileDto> accountDtoList = accountList.stream()
                .map(AccountProfileDto::new)
                .toList();
        paginatedData = new PaginatedDataDto<>(accountDtoList, accountDtoList.size());
    }

    @Test
    void search_Success() {
        // Arrange
        String query = "test";
        AccountType accountType = AccountType.PAYMENT;
        AccountStatus status = AccountStatus.ACTIVE;
        int limit = 10;
        int offset = 0;

        when(accountRepository.search(eq(query), eq(accountType), eq(status), eq(limit), eq(offset)))
                .thenReturn(paginatedData);

        // Act
        PaginatedDataDto<AccountProfileDto> result = accountService.search(testEmployee, query, accountType, status, limit, offset);

        // Assert
        assertNotNull(result);
        assertEquals(accountList.size(), result.getTotalCount());
        verify(accountRepository, times(1)).search(eq(query), eq(accountType), eq(status), eq(limit), eq(offset));
    }

    @Test
    void search_DefaultParameters() {
        // Arrange
        when(accountRepository.search(eq(""), eq(null), eq(null), eq(10), eq(0)))
                .thenReturn(paginatedData);

        // Act
        PaginatedDataDto<AccountProfileDto> result = accountService.search(testEmployee, null, null, null, 0, 0);

        // Assert
        assertNotNull(result);
        assertEquals(accountList.size(), result.getTotalCount());
        verify(accountRepository, times(1)).search(eq(""), eq(null), eq(null), eq(10), eq(0));
    }

    @Test
    void search_Unauthorized() {
        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> accountService.search(null, "", null, null, 10, 0));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("User not authenticated", exception.getReason());
    }

    @Test
    void search_Forbidden_NonEmployee() {
        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> accountService.search(testCustomer, "", null, null, 10, 0));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("You do not have permission to view accounts", exception.getReason());
    }

    @Test
    void getAccountByIban_Success_Owner() {
        // Arrange
        when(accountRepository.findByIban(testAccount.getIban())).thenReturn(testAccount);

        // Act
        Account result = accountService.getAccountByIban(testCustomer, testAccount.getIban());

        // Assert
        assertNotNull(result);
        assertEquals(testAccount.getIban(), result.getIban());
        verify(accountRepository, times(1)).findByIban(testAccount.getIban());
    }

    @Test
    void getAccountByIban_Success_Employee() {
        // Arrange
        when(accountRepository.findByIban(testAccount.getIban())).thenReturn(testAccount);

        // Act
        Account result = accountService.getAccountByIban(testEmployee, testAccount.getIban());

        // Assert
        assertNotNull(result);
        assertEquals(testAccount.getIban(), result.getIban());
        verify(accountRepository, times(1)).findByIban(testAccount.getIban());
    }

    @Test
    void getAccountByIban_Unauthorized() {
        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> accountService.getAccountByIban(null, testAccount.getIban()));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("User not authenticated", exception.getReason());
    }

    @Test
    void getAccountByIban_NotFound() {
        // Arrange
        String nonExistentIban = "NL91SPYD9999999999";
        when(accountRepository.findByIban(nonExistentIban)).thenReturn(null);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> accountService.getAccountByIban(testEmployee, nonExistentIban));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Account not found with IBAN: " + nonExistentIban, exception.getReason());
    }

    @Test
    void getAccountByIban_Forbidden_OtherCustomer() {
        // Arrange
        User otherCustomer = new User(
                "Other",
                "Customer",
                "password",
                "other@example.com",
                111222333,
                "111-222-3333",
                UserRole.APPROVED,
                new ArrayList<>()
        );
        otherCustomer.setUserId(3L);

        when(accountRepository.findByIban(testAccount.getIban())).thenReturn(testAccount);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> accountService.getAccountByIban(otherCustomer, testAccount.getIban()));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("You do not have permission to view this account", exception.getReason());
    }

    @Test
    void updateBalanceLimits_Success() {
        // Arrange
        Long accountId = testAccount.getAccountId();
        BigDecimal newDailyLimit = BigDecimal.valueOf(2000.00);
        BigDecimal newAbsoluteLimit = BigDecimal.valueOf(10000.00);
        BigDecimal newBalanceLimit = BigDecimal.valueOf(-500.00);

        when(accountRepository.findByAccountId(accountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // Act
        Account result = accountService.updateBalanceLimits(testEmployee, accountId, newDailyLimit, newAbsoluteLimit, newBalanceLimit);

        // Assert
        assertNotNull(result);
        assertEquals(newDailyLimit, result.getDailyLimit());
        assertEquals(newAbsoluteLimit, result.getAbsoluteLimit());
        verify(accountRepository, times(1)).findByAccountId(accountId);
        verify(accountRepository, times(1)).save(testAccount);
    }

    @Test
    void updateBalanceLimits_Unauthorized() {
        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> accountService.updateBalanceLimits(null, 1L, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("User not authenticated", exception.getReason());
    }

    @Test
    void updateBalanceLimits_Forbidden_NonEmployee() {
        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> accountService.updateBalanceLimits(testCustomer, 1L, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("You do not have permission to update account limits", exception.getReason());
    }

    @Test
    void createAccount_Success() {
        // Arrange
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setAccountId(3L);
            return account;
        });

        // Act
        Account result = accountService.createAccount(
                testCustomer,
                AccountType.PAYMENT,
                new BigDecimal("1000.00"),
                new BigDecimal("5000.00"),
                BigDecimal.ZERO,
                new BigDecimal("10000.00")
        );

        // Assert
        assertNotNull(result);
        assertNotNull(result.getAccountId());
        assertEquals(testCustomer, result.getUser());
        assertEquals(AccountType.PAYMENT, result.getAccountType());
        assertEquals(AccountStatus.ACTIVE, result.getStatus());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void updateAccount_Success() {
        // Arrange
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // Act
        accountService.updateAccount(testAccount);

        // Assert
        verify(accountRepository, times(1)).save(testAccount);
    }

    @Test
    void saveAll_Success() {
        // Arrange
        when(accountRepository.saveAll(anyList())).thenReturn(accountList);

        // Act
        accountService.saveAll(accountList);

        // Assert
        verify(accountRepository, times(1)).saveAll(accountList);
    }
}

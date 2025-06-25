package com.stefvisser.springyield.controllers;

import com.stefvisser.springyield.dto.AccountLimitsDto;
import com.stefvisser.springyield.dto.AccountProfileDto;
import com.stefvisser.springyield.dto.PaginatedDataDto;
import com.stefvisser.springyield.models.Account;
import com.stefvisser.springyield.models.AccountStatus;
import com.stefvisser.springyield.models.AccountType;
import com.stefvisser.springyield.models.User;
import com.stefvisser.springyield.models.UserRole;
import com.stefvisser.springyield.services.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AccountControllerTest {

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

    private User testEmployee;
    private User testCustomer;
    private Account testAccount;
    private AccountProfileDto testAccountProfileDto;
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

        // Setup test account
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
                new ArrayList<>()
        );

        // Setup test account profile DTO
        testAccountProfileDto = new AccountProfileDto(testAccount);

        // Setup paginated data
        List<AccountProfileDto> accountList = new ArrayList<>();
        accountList.add(testAccountProfileDto);
        paginatedData = new PaginatedDataDto<>(accountList, 1);
    }

    @Test
    void search_Success() {
        // Arrange
        String query = "test";
        AccountType accountType = AccountType.PAYMENT;
        AccountStatus status = AccountStatus.ACTIVE;
        int limit = 10;
        int offset = 0;

        when(accountService.search(any(User.class), eq(query), eq(accountType), eq(status), eq(limit), eq(offset)))
                .thenReturn(paginatedData);

        // Act
        ResponseEntity<?> response = accountController.search(testEmployee, query, accountType, status, limit, offset);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof PaginatedDataDto);
        PaginatedDataDto<AccountProfileDto> returnedData = (PaginatedDataDto<AccountProfileDto>) response.getBody();
        assertEquals(1, returnedData.getTotalCount());
        assertEquals(1, returnedData.getData().size());
    }

    @Test
    void getAccountByIban_Success() {
        // Arrange
        String iban = "NL91SPYD0000000001";
        when(accountService.getAccountByIban(any(User.class), eq(iban))).thenReturn(testAccount);

        // Act
        ResponseEntity<?> response = accountController.getAccountByIban(testCustomer, iban);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof Account);
        Account returnedAccount = (Account) response.getBody();
        assertEquals(iban, returnedAccount.getIban());
    }

    @Test
    void getAccountByIban_NotFound() {
        // Arrange
        String iban = "NL91SPYD9999999999";
        when(accountService.getAccountByIban(any(User.class), eq(iban)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found with IBAN: " + iban));

        // Act
        ResponseEntity<?> response = accountController.getAccountByIban(testCustomer, iban);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Account not found with IBAN: " + iban, response.getBody());
    }

    @Test
    void updateBalanceLimits_Success() {
        // Arrange
        Long accountId = 1L;
        AccountLimitsDto limitsDto = new AccountLimitsDto();
        limitsDto.setDailyLimit(new BigDecimal("2000.00"));
        limitsDto.setAbsoluteLimit(new BigDecimal("10000.00"));

        when(accountService.updateBalanceLimits(
                any(User.class), eq(accountId), eq(limitsDto.getDailyLimit()), eq(limitsDto.getAbsoluteLimit())))
                .thenReturn(testAccount);

        // Act
        ResponseEntity<?> response = accountController.updateBalanceLimits(testEmployee, accountId, limitsDto);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof AccountProfileDto);
        AccountProfileDto returnedDto = (AccountProfileDto) response.getBody();
        assertEquals(testAccount.getAccountId(), returnedDto.getAccountId());
    }

    @Test
    void updateBalanceLimits_Forbidden() {
        // Arrange
        Long accountId = 1L;
        AccountLimitsDto limitsDto = new AccountLimitsDto();
        limitsDto.setDailyLimit(new BigDecimal("2000.00"));
        limitsDto.setAbsoluteLimit(new BigDecimal("10000.00"));

        when(accountService.updateBalanceLimits(
                any(User.class), eq(accountId), any(BigDecimal.class), any(BigDecimal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to update account limits"));

        // Act
        ResponseEntity<?> response = accountController.updateBalanceLimits(testCustomer, accountId, limitsDto);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("You do not have permission to update account limits", response.getBody());
    }
}

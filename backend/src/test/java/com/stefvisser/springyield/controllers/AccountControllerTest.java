package com.stefvisser.springyield.controllers;

import com.stefvisser.springyield.dto.AccountProfileDto;
import com.stefvisser.springyield.dto.PaginatedDataDTO;
import com.stefvisser.springyield.models.*;
import com.stefvisser.springyield.services.AccountService;
import com.stefvisser.springyield.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AccountControllerTest {

    @Mock
    private AccountService accountService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AccountController accountController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAccountsBySearch_ShouldReturnOkForEmployee() {
        User employee = new User();
        employee.setUserId(1L);
        employee.setRole(UserRole.EMPLOYEE);

        when(userService.getUserById(1L)).thenReturn(employee);
        PaginatedDataDTO<AccountProfileDto> paginatedData = new PaginatedDataDTO<>(Collections.emptyList(), 0);
        when(accountService.searchAccount(anyString(), any(), any(), anyInt(), anyInt())).thenReturn(paginatedData);

        ResponseEntity<?> response = accountController.getAccountsBySearch(employee, "query", AccountType.PAYMENT, AccountStatus.ACTIVE, 10, 0);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(paginatedData, response.getBody());
    }

    @Test
    void getAccountsBySearch_ShouldReturnUnauthorizedIfUserNull() {
        ResponseEntity<?> response = accountController.getAccountsBySearch(null, "query", AccountType.PAYMENT, AccountStatus.ACTIVE, 10, 0);
        assertEquals(401, response.getStatusCodeValue());
    }

    @Test
    void getAccountsBySearch_ShouldReturnForbiddenIfNotEmployee() {
        User customer = new User();
        customer.setUserId(2L);
        customer.setRole(UserRole.APPROVED);

        when(userService.getUserById(2L)).thenReturn(customer);

        ResponseEntity<?> response = accountController.getAccountsBySearch(customer, "query", AccountType.PAYMENT, AccountStatus.ACTIVE, 10, 0);
        assertEquals(403, response.getStatusCodeValue());
    }
}
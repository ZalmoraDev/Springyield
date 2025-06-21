package com.stefvisser.springyield.services;

import com.stefvisser.springyield.dto.AccountProfileDto;
import com.stefvisser.springyield.dto.PaginatedDataDTO;
import com.stefvisser.springyield.models.Account;
import com.stefvisser.springyield.models.AccountType;
import com.stefvisser.springyield.models.User;
import com.stefvisser.springyield.models.UserRole;
import com.stefvisser.springyield.repositories.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    private Account sampleAccount;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User("Test", "User", "password", "test@example.com", 12345667, "06", UserRole.APPROVED, new ArrayList<>());
        sampleUser.setUserId(1L);

        sampleAccount = new Account();
        sampleAccount.setIban("NL01INHO0000000001");
        sampleAccount.setAccountType(AccountType.PAYMENT);
        sampleAccount.setBalance(BigDecimal.valueOf(1000));
        sampleAccount.setUser(sampleUser);
        sampleAccount.setAbsoluteLimit(BigDecimal.valueOf(0));
        sampleAccount.setDailyLimit(BigDecimal.valueOf(500));

    }

    @Test
    void getAllAccounts_returnsListOfAccounts() {
        List<Account> accounts = new ArrayList<>();
        accounts.add(sampleAccount);
        when(accountRepository.findAll()).thenReturn(accounts);

        List<Account> result = accountService.getAllAccounts();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("NL01INHO0000000001", result.get(0).getIban());
        verify(accountRepository).findAll();
    }

    @Test
    void getAccountsByUserId_whenUserHasAccounts_returnsListOfAccounts() {
        List<Account> accounts = new ArrayList<>();
        accounts.add(sampleAccount);
        when(accountRepository.findByUserUserId(1L)).thenReturn(accounts);

        List<Account> result = accountService.getAccountsByUserId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(sampleUser.getUserId(), result.get(0).getUser().getUserId());
        verify(accountRepository).findByUserUserId(1L);
    }

    @Test
    void getAccountsByUserId_whenUserHasNoAccounts_returnsEmptyList() {
        when(accountRepository.findByUserUserId(2L)).thenReturn(new ArrayList<>());

        List<Account> result = accountService.getAccountsByUserId(2L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(accountRepository).findByUserUserId(2L);
    }

    @Test
    void getAccountByIban_whenAccountExists_returnsAccount() {
        when(accountRepository.findByIban("NL01INHO0000000001")).thenReturn(sampleAccount);

        Account result = accountService.getAccountByIban("NL01INHO0000000001");

        assertNotNull(result);
        assertEquals("NL01INHO0000000001", result.getIban());
        verify(accountRepository).findByIban("NL01INHO0000000001");
    }

    @Test
    void getAccountByIban_whenAccountDoesNotExist_returnsNull() {
        when(accountRepository.findByIban(anyString())).thenReturn(null);

        Account result = accountService.getAccountByIban("NL02INHO0000000002");

        assertNull(result);
        verify(accountRepository).findByIban("NL02INHO0000000002");
    }

    @Test
    void updateAccount_callsRepositorySave() {
        // No need to mock accountRepository.save(account) if it returns void or the same account
        // We just verify it's called
        accountService.updateAccount(sampleAccount);
        verify(accountRepository).save(sampleAccount);
    }

    @Test
    void createAccount_callsRepositorySaveAndReturnsAccount() {
        when(accountRepository.save(any(Account.class))).thenReturn(sampleAccount);

        Account createdAccount = accountService.createAccount(sampleAccount);

        assertNotNull(createdAccount);
        assertEquals(sampleAccount.getIban(), createdAccount.getIban());
        verify(accountRepository).save(sampleAccount);
    }

    @Test
    void searchAccount_callsRepositorySearch() {
        String query = "test";
        AccountType type = AccountType.PAYMENT;
        int limit = 5;
        int offset = 0;
        PaginatedDataDTO<AccountProfileDto> expectedResponse = new PaginatedDataDTO<>(new ArrayList<>(), 0);

        when(accountRepository.searchAccount(query, type, null, limit, offset)).thenReturn(expectedResponse);

        PaginatedDataDTO<AccountProfileDto> result = accountService.searchAccount(query, type, null, limit, offset);

        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(accountRepository).searchAccount(query, type, null, limit, offset);
    }

    @Test
    void validateTransaction_alwaysReturnsFalse() {
        // As per current implementation, this method always returns false.
        boolean isValid = accountService.validateTransaction(sampleAccount, new com.stefvisser.springyield.models.Transaction());
        assertFalse(isValid);
    }
}


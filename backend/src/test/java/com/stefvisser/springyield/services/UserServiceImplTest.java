package com.stefvisser.springyield.services;

import com.stefvisser.springyield.dto.PaginatedDataDto;
import com.stefvisser.springyield.dto.UserProfileDto;
import com.stefvisser.springyield.dto.UserUpdateDto;
import com.stefvisser.springyield.models.Account;
import com.stefvisser.springyield.models.AccountStatus;
import com.stefvisser.springyield.models.AccountType;
import com.stefvisser.springyield.models.User;
import com.stefvisser.springyield.models.UserRole;
import com.stefvisser.springyield.repositories.AccountRepository;
import com.stefvisser.springyield.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User testEmployee;
    private User testCustomer;
    private User testUnapprovedUser;
    private Account testAccount;
    private List<User> userList;
    private PaginatedDataDto<UserProfileDto> paginatedData;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup test employee
        testEmployee = new User(
                "Employee",
                "Admin",
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
                "John",
                "Doe",
                "password",
                "customer@example.com",
                987654321,
                "987-654-3210",
                UserRole.APPROVED,
                new ArrayList<>()
        );
        testCustomer.setUserId(2L);

        // Setup test unapproved user
        testUnapprovedUser = new User(
                "New",
                "User",
                "password",
                "newuser@example.com",
                111222333,
                "111-222-3333",
                UserRole.UNAPPROVED,
                new ArrayList<>()
        );
        testUnapprovedUser.setUserId(3L);

        // Setup test account
        testAccount = new Account();
        testAccount.setAccountId(1L);
        testAccount.setAccountType(AccountType.PAYMENT);
        testAccount.setUser(testCustomer);
        testAccount.setStatus(AccountStatus.ACTIVE);

        testCustomer.getAccounts().add(testAccount);

        // Setup user list and paginated data
        userList = Arrays.asList(testEmployee, testCustomer, testUnapprovedUser);
        List<UserProfileDto> dtoList = userList.stream()
                .map(UserProfileDto::new)
                .toList();
        paginatedData = new PaginatedDataDto<>(dtoList, dtoList.size());
    }

    @Test
    void getUserById_SelfAccess_Success() {
        // Arrange
        when(userRepository.findByUserId(testCustomer.getUserId())).thenReturn(testCustomer);

        // Act
        User result = userService.getUserById(testCustomer, testCustomer.getUserId());

        // Assert
        assertNotNull(result);
        assertEquals(testCustomer.getUserId(), result.getUserId());
        verify(userRepository, times(1)).findByUserId(testCustomer.getUserId());
    }

    @Test
    void getUserById_EmployeeAccess_Success() {
        // Arrange
        when(userRepository.findByUserId(testCustomer.getUserId())).thenReturn(testCustomer);

        // Act
        User result = userService.getUserById(testEmployee, testCustomer.getUserId());

        // Assert
        assertNotNull(result);
        assertEquals(testCustomer.getUserId(), result.getUserId());
        verify(userRepository, times(1)).findByUserId(testCustomer.getUserId());
    }

    @Test
    void getUserById_Unauthorized() {
        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userService.getUserById(null, testCustomer.getUserId()));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void getUserById_Forbidden_NonEmployeeAccessingOtherUser() {
        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userService.getUserById(testCustomer, testEmployee.getUserId()));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void getUserById_NotFound() {
        // Arrange
        when(userRepository.findByUserId(anyLong())).thenReturn(null);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userService.getUserById(testEmployee, 999L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void search_Success() {
        // Arrange
        String query = "John";
        UserRole role = UserRole.APPROVED;
        int limit = 10;
        int offset = 0;

        when(userRepository.search(eq(query), eq(role), eq(limit), eq(offset), eq(false)))
                .thenReturn(paginatedData);

        // Act
        PaginatedDataDto<UserProfileDto> result = userService.search(testEmployee, query, role, limit, offset);

        // Assert
        assertNotNull(result);
        assertEquals(userList.size(), result.getTotalCount());
        verify(userRepository, times(1)).search(eq(query), eq(role), eq(limit), eq(offset), eq(false));
    }

    @Test
    void search_Unauthorized() {
        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userService.search(null, "", null, 10, 0));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void search_Forbidden_NonEmployee() {
        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userService.search(testCustomer, "", null, 10, 0));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void approveUser_Success() {
        // Arrange
        BigDecimal dailyLimit = new BigDecimal("1000.00");
        BigDecimal absoluteLimit = new BigDecimal("5000.00");

        when(userRepository.findByUserId(testUnapprovedUser.getUserId())).thenReturn(testUnapprovedUser);
        when(accountService.createAccount(eq(testUnapprovedUser), any(AccountType.class),
                any(BigDecimal.class), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(new Account());

        // Act
        userService.approveUser(testEmployee, testUnapprovedUser.getUserId(), dailyLimit, absoluteLimit);

        // Assert
        assertEquals(UserRole.APPROVED, testUnapprovedUser.getRole());
        verify(userRepository, times(1)).save(testUnapprovedUser);
        verify(accountService, times(2)).createAccount(eq(testUnapprovedUser), any(AccountType.class),
                any(BigDecimal.class), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class));
    }

    @Test
    void approveUser_Unauthorized() {
        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userService.approveUser(null, testUnapprovedUser.getUserId(),
                        BigDecimal.ONE, BigDecimal.ONE));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void approveUser_Forbidden_NonEmployee() {
        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userService.approveUser(testCustomer, testUnapprovedUser.getUserId(),
                        BigDecimal.ONE, BigDecimal.ONE));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void updateUser_Success() {
        // Arrange
        UserUpdateDto updateDto = new UserUpdateDto();
        updateDto.setFirstName("Updated");
        updateDto.setLastName("Name");

        when(userRepository.findByUserId(testCustomer.getUserId())).thenReturn(testCustomer);
        when(userRepository.save(any(User.class))).thenReturn(testCustomer);

        // Act
        UserProfileDto result = userService.updateUser(testCustomer, testCustomer.getUserId(), updateDto);

        // Assert
        assertNotNull(result);
        verify(userRepository, times(1)).save(testCustomer);
    }

    @Test
    void updateUser_AdminRoleChange_Success() {
        // Arrange
        User testAdmin = new User(
                "Admin",
                "User",
                "password",
                "admin@example.com",
                555555555,
                "555-555-5555",
                UserRole.ADMIN,
                new ArrayList<>()
        );
        testAdmin.setUserId(4L);

        UserUpdateDto updateDto = new UserUpdateDto();
        updateDto.setRole(UserRole.EMPLOYEE);

        when(userRepository.findByUserId(testCustomer.getUserId())).thenReturn(testCustomer);
        when(userRepository.save(any(User.class))).thenReturn(testCustomer);

        // Act
        UserProfileDto result = userService.updateUser(testAdmin, testCustomer.getUserId(), updateDto);

        // Assert
        assertNotNull(result);
        verify(userRepository, times(1)).save(testCustomer);
    }

    @Test
    void updateUser_Forbidden_NonAdminRoleChange() {
        // Arrange
        UserUpdateDto updateDto = new UserUpdateDto();
        updateDto.setRole(UserRole.EMPLOYEE);

        when(userRepository.findByUserId(testCustomer.getUserId())).thenReturn(testCustomer);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userService.updateUser(testEmployee, testCustomer.getUserId(), updateDto));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void deleteUser_Success() {
        // Arrange
        when(userRepository.findByUserId(testCustomer.getUserId())).thenReturn(testCustomer);

        // Act
        userService.deleteUser(testEmployee, testCustomer.getUserId());

        // Assert
        verify(userRepository, times(1)).delete(testCustomer);
        verify(accountService, times(1)).saveAll(anyList());
    }

    @Test
    void deleteUser_SelfDelete_Success() {
        // Arrange
        when(userRepository.findByUserId(testCustomer.getUserId())).thenReturn(testCustomer);

        // Act
        userService.deleteUser(testCustomer, testCustomer.getUserId());

        // Assert
        verify(userRepository, times(1)).delete(testCustomer);
    }

    @Test
    void deleteUser_Forbidden_EmployeeSelfDelete() {
        // Arrange
        when(userRepository.findByUserId(testEmployee.getUserId())).thenReturn(testEmployee);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userService.deleteUser(testEmployee, testEmployee.getUserId()));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void findByEmail_Success() {
        // Arrange
        when(userRepository.findByEmail(testCustomer.getEmail())).thenReturn(testCustomer);

        // Act
        User result = userService.findByEmail(testCustomer.getEmail());

        // Assert
        assertNotNull(result);
        assertEquals(testCustomer.getEmail(), result.getEmail());
        verify(userRepository, times(1)).findByEmail(testCustomer.getEmail());
    }

    @Test
    void save_Success() {
        // Arrange
        when(userRepository.save(any(User.class))).thenReturn(testCustomer);

        // Act
        userService.save(testCustomer);

        // Assert
        verify(userRepository, times(1)).save(testCustomer);
    }

    @Test
    void saveAll_Success() {
        // Arrange
        when(userRepository.saveAll(anyList())).thenReturn(userList);

        // Act
        userService.saveAll(userList);

        // Assert
        verify(userRepository, times(1)).saveAll(userList);
    }
}

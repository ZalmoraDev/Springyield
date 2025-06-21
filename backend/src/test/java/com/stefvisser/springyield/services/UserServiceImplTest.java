package com.stefvisser.springyield.services;

import com.stefvisser.springyield.dto.PaginatedDataDTO;
import com.stefvisser.springyield.dto.UserLoginDto;
import com.stefvisser.springyield.dto.UserProfileDto;
import com.stefvisser.springyield.dto.UserSignupDto;
import com.stefvisser.springyield.models.Account; // Assuming Account model is needed for User constructor
import com.stefvisser.springyield.models.User;
import com.stefvisser.springyield.models.UserRole;
import com.stefvisser.springyield.repositories.TransactionRepository;
import com.stefvisser.springyield.repositories.UserRepository;
import com.stefvisser.springyield.utils.FakeUserLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private AccountService accountService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private FakeUserLoader fakeUserLoader;

    @InjectMocks
    private UserServiceImpl userService;

    private User sampleUser;
    private UserProfileDto sampleUserProfileDto;

    @BeforeEach
    void setUp() {
        sampleUser = new User(
                "Test", "User", "encodedPassword", "test.user@example.com",
                123456789, "0612345678", UserRole.APPROVED, new ArrayList<Account>()
        );
        sampleUser.setUserId(1L);

        sampleUserProfileDto = new UserProfileDto(sampleUser);
    }

    @Test
    void getUserById_whenUserExists_returnsUser() {
        when(userRepository.findByUserId(1L)).thenReturn(sampleUser);

        User foundUser = userService.getUserById(1L);

        assertNotNull(foundUser);
        assertEquals("test.user@example.com", foundUser.getEmail());
        verify(userRepository).findByUserId(1L);
    }

    @Test
    void getUserById_whenUserDoesNotExist_returnsNull() {
        when(userRepository.findByUserId(any(Long.class))).thenReturn(null);

        User foundUser = userService.getUserById(99L);

        assertNull(foundUser);
        verify(userRepository).findByUserId(99L);
    }

    @Test
    void login_whenCredentialsAreValid_returnsUserProfileDto() {
        UserLoginDto loginDto = new UserLoginDto();
        loginDto.setEmail("test.user@example.com");
        loginDto.setPassword("password123");

        BCryptPasswordEncoder realEncoder = userService.getPasswordEncoder();
        sampleUser.setPassword(realEncoder.encode("password123"));
        when(userRepository.findByEmail("test.user@example.com")).thenReturn(sampleUser);

        UserProfileDto result = userService.login(loginDto);

        assertNotNull(result);
        assertEquals("test.user@example.com", result.getEmail());
        verify(userRepository).findByEmail("test.user@example.com");
    }

    @Test
    void login_whenUserNotFound_returnsNull() {
        UserLoginDto loginDto = new UserLoginDto();
        loginDto.setEmail("nonexistent@example.com");
        loginDto.setPassword("password123");

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(null);

        UserProfileDto result = userService.login(loginDto);

        assertNull(result);
        verify(userRepository).findByEmail("nonexistent@example.com");
    }

    @Test
    void login_whenPasswordIsIncorrect_returnsNull() {
        UserLoginDto loginDto = new UserLoginDto();
        loginDto.setEmail("test.user@example.com");
        loginDto.setPassword("wrongPassword");

        BCryptPasswordEncoder realEncoder = userService.getPasswordEncoder();
        sampleUser.setPassword(realEncoder.encode("correctPassword"));
        when(userRepository.findByEmail("test.user@example.com")).thenReturn(sampleUser);

        UserProfileDto result = userService.login(loginDto);

        assertNull(result);
        verify(userRepository).findByEmail("test.user@example.com");
    }

    @Test
    void signupUser_whenEmailDoesNotExist_createsAndReturnsUser() {
        UserSignupDto signupDto = new UserSignupDto();
        signupDto.setEmail("new.user@example.com");
        signupDto.setPassword("password123");
        signupDto.setFirstName("New");
        signupDto.setLastName("User");
        signupDto.setBsnNumber(987654321);
        signupDto.setPhoneNumber("0687654321");

        when(userRepository.existsByEmail("new.user@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User userToSave = invocation.getArgument(0);
            userToSave.setUserId(2L);
            return userToSave;
        });

        User createdUser = userService.signupUser(signupDto);

        assertNotNull(createdUser);
        assertEquals("new.user@example.com", createdUser.getEmail());
        assertEquals(UserRole.UNAPPROVED, createdUser.getRole());
        verify(userRepository).existsByEmail("new.user@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void signupUser_whenEmailAlreadyExists_returnsNull() {
        UserSignupDto signupDto = new UserSignupDto();
        signupDto.setEmail("test.user@example.com");
        signupDto.setPassword("password123");
        signupDto.setFirstName("Test");
        signupDto.setLastName("User");
        signupDto.setBsnNumber(123456789);
        signupDto.setPhoneNumber("0612345678");

        when(userRepository.existsByEmail("test.user@example.com")).thenReturn(true);

        User createdUser = userService.signupUser(signupDto);

        assertNull(createdUser);
        verify(userRepository).existsByEmail("test.user@example.com");
    }

    @Test
    void approveUser_whenUserExistsAndUnapproved_approvesAndCreatesAccounts() {
        User unapprovedUser = new User("Unapproved", "User", "pass", "unapproved@test.com", 123456789, "0612345678", UserRole.UNAPPROVED, new ArrayList<Account>());
        unapprovedUser.setUserId(2L);

        when(userRepository.findByUserId(2L)).thenReturn(unapprovedUser);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountService.createAccount(any(Account.class))).thenAnswer(invocation -> {
            Account acc = invocation.getArgument(0);
            return acc;
        });

        BigDecimal dailyLimit = new BigDecimal("3000.00");
        BigDecimal absoluteLimit = new BigDecimal("1000.00");

        UserProfileDto approvedUserProfile = userService.approveUser(2L, dailyLimit, absoluteLimit);

        assertNotNull(approvedUserProfile);
        assertEquals(UserRole.APPROVED, approvedUserProfile.getRole());
        assertNotNull(approvedUserProfile.getAccounts());
        verify(userRepository).findByUserId(2L);
        verify(userRepository).save(any(User.class));
        verify(accountService, org.mockito.Mockito.times(2)).createAccount(any(Account.class));
    }

    @Test
    void approveUser_whenUserNotFound_throwsRuntimeException() {
        when(userRepository.findByUserId(3L)).thenReturn(null);

        BigDecimal dailyLimit = new BigDecimal("3000.00");
        BigDecimal absoluteLimit = new BigDecimal("1000.00");

        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.approveUser(3L, dailyLimit, absoluteLimit);
        });

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findByUserId(3L);
    }

    @Test
    void approveUser_whenUserAlreadyApproved_throwsRuntimeException() {
        User alreadyApprovedUser = new User("Approved", "User", "pass", "approved@test.com", 987654321, "0687654321", UserRole.APPROVED, new ArrayList<Account>());
        alreadyApprovedUser.setUserId(4L);

        when(userRepository.findByUserId(4L)).thenReturn(alreadyApprovedUser);

        BigDecimal dailyLimit = new BigDecimal("3000.00");
        BigDecimal absoluteLimit = new BigDecimal("1000.00");

        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.approveUser(4L, dailyLimit, absoluteLimit);
        });

        assertEquals("User is already approved", exception.getMessage());
        verify(userRepository).findByUserId(4L);
    }

    @Test
    void deleteUser_whenUserExists_deletesUser() {
        when(userRepository.findByUserId(1L)).thenReturn(sampleUser);

        assertDoesNotThrow(() -> userService.deleteUser(1L));

        verify(userRepository).findByUserId(1L);
        verify(userRepository).delete(sampleUser);
    }

    @Test
    void deleteUser_whenUserNotFound_throwsRuntimeException() {
        when(userRepository.findByUserId(99L)).thenReturn(null);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.deleteUser(99L);
        });

        assertEquals("User not found with id: 99", exception.getMessage());
        verify(userRepository).findByUserId(99L);
    }

    @Test
    void getAllUsers_returnsListOfUsers() {
        List<User> users = new ArrayList<>();
        users.add(sampleUser);
        users.add(new User("Another", "User", "pass", "another@example.com", 1234567, "06", UserRole.EMPLOYEE, new ArrayList<>()));

        when(userRepository.findAll()).thenReturn(users);

        List<User> result = userService.getAllUsers();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(userRepository).findAll();
    }

    @Test
    void search_callsRepositorySearch() {
        String query = "test";
        UserRole role = UserRole.APPROVED;
        int limit = 10;
        int offset = 0;
        PaginatedDataDTO<UserProfileDto> expectedResponse = new PaginatedDataDTO<>(new ArrayList<>(), 0);

        when(userRepository.search(query, role, limit, offset, false)).thenReturn(expectedResponse);

        PaginatedDataDTO<UserProfileDto> result = userService.search(query, role, limit, offset, false);

        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(userRepository).search(query, role, limit, offset, false);
    }

    @Test
    void getPasswordEncoder_returnsEncoder() {
        BCryptPasswordEncoder encoder = userService.getPasswordEncoder();
        assertNotNull(encoder);
    }


}


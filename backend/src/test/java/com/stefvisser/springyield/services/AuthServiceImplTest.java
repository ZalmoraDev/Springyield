package com.stefvisser.springyield.services;

import com.stefvisser.springyield.dto.AuthSessionDto;
import com.stefvisser.springyield.dto.UserLoginDto;
import com.stefvisser.springyield.dto.UserProfileDto;
import com.stefvisser.springyield.dto.UserSignupDto;
import com.stefvisser.springyield.models.User;
import com.stefvisser.springyield.models.UserRole;
import com.stefvisser.springyield.repositories.AuthRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.security.Key;
import java.util.ArrayList;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {

    @Mock
    private AuthRepository authRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private UserLoginDto validLoginDto;
    private UserSignupDto validSignupDto;
    private User testUser;
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        passwordEncoder = new BCryptPasswordEncoder();

        // Setup valid login dto
        validLoginDto = new UserLoginDto();
        validLoginDto.setEmail("test@example.com");
        validLoginDto.setPassword("password123");

        // Setup valid signup dto
        validSignupDto = new UserSignupDto();
        validSignupDto.setFirstName("John");
        validSignupDto.setLastName("Doe");
        validSignupDto.setEmail("john.doe@example.com");
        validSignupDto.setPassword("securepassword");
        validSignupDto.setBsnNumber(123456789);
        validSignupDto.setPhoneNumber("123-456-7890");

        // Setup test user
        testUser = new User(
                "John",
                "Doe",
                passwordEncoder.encode("password123"),
                "test@example.com",
                123456789,
                "123-456-7890",
                UserRole.APPROVED,
                new ArrayList<>()
        );
        testUser.setUserId(1L);
    }

    @Test
    void signup_Success() {
        // Arrange
        when(authRepository.existsByEmail(validSignupDto.getEmail())).thenReturn(false);
        when(authRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(1L);
            return user;
        });

        // Act
        AuthSessionDto result = authService.signup(validSignupDto);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getToken());
        assertNotNull(result.getUser());
        assertEquals(validSignupDto.getEmail(), result.getUser().getEmail());
        assertEquals(validSignupDto.getFirstName(), result.getUser().getFirstName());
        assertEquals(validSignupDto.getLastName(), result.getUser().getLastName());
        assertEquals(UserRole.UNAPPROVED, result.getUser().getRole());
        verify(authRepository, times(1)).existsByEmail(validSignupDto.getEmail());
        verify(authRepository, times(1)).save(any(User.class));
    }

    @Test
    void signup_EmailAlreadyExists() {
        // Arrange
        when(authRepository.existsByEmail(validSignupDto.getEmail())).thenReturn(true);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.signup(validSignupDto));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("This email is already registered", exception.getReason());
        verify(authRepository, times(1)).existsByEmail(validSignupDto.getEmail());
        verify(authRepository, never()).save(any(User.class));
    }

    @Test
    void signup_MissingRequiredFields() {
        // Act & Assert - Null DTO
        ResponseStatusException exception1 = assertThrows(ResponseStatusException.class,
                () -> authService.signup(null));

        assertEquals(HttpStatus.BAD_REQUEST, exception1.getStatusCode());
        assertEquals("Not all required fields are filled in", exception1.getReason());

        // Act & Assert - Missing Email
        UserSignupDto missingEmailDto = new UserSignupDto();
        missingEmailDto.setPassword("password123");

        ResponseStatusException exception2 = assertThrows(ResponseStatusException.class,
                () -> authService.signup(missingEmailDto));

        assertEquals(HttpStatus.BAD_REQUEST, exception2.getStatusCode());
        assertEquals("Not all required fields are filled in", exception2.getReason());

        // Act & Assert - Missing Password
        UserSignupDto missingPasswordDto = new UserSignupDto();
        missingPasswordDto.setEmail("test@example.com");

        ResponseStatusException exception3 = assertThrows(ResponseStatusException.class,
                () -> authService.signup(missingPasswordDto));

        assertEquals(HttpStatus.BAD_REQUEST, exception3.getStatusCode());
        assertEquals("Not all required fields are filled in", exception3.getReason());
    }

    @Test
    void login_Success() {
        // Arrange
        when(authRepository.findByEmail(validLoginDto.getEmail())).thenReturn(testUser);

        // Act
        AuthSessionDto result = authService.login(validLoginDto);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getToken());
        assertNotNull(result.getUser());
        assertEquals(testUser.getUserId(), result.getUser().getUserId());
        assertEquals(testUser.getEmail(), result.getUser().getEmail());
        assertEquals(testUser.getFirstName(), result.getUser().getFirstName());
        assertEquals(testUser.getLastName(), result.getUser().getLastName());
        assertEquals(testUser.getRole(), result.getUser().getRole());
        verify(authRepository, times(1)).findByEmail(validLoginDto.getEmail());
    }

    @Test
    void login_UserNotFound() {
        // Arrange
        when(authRepository.findByEmail(validLoginDto.getEmail())).thenReturn(null);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.login(validLoginDto));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Invalid email or password", exception.getReason());
        verify(authRepository, times(1)).findByEmail(validLoginDto.getEmail());
    }

    @Test
    void login_InvalidPassword() {
        // Arrange
        UserLoginDto wrongPasswordDto = new UserLoginDto();
        wrongPasswordDto.setEmail(validLoginDto.getEmail());
        wrongPasswordDto.setPassword("wrongpassword");

        when(authRepository.findByEmail(wrongPasswordDto.getEmail())).thenReturn(testUser);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.login(wrongPasswordDto));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Invalid email or password", exception.getReason());
        verify(authRepository, times(1)).findByEmail(wrongPasswordDto.getEmail());
    }

    @Test
    void generateJwtToken_ValidToken() {
        // Arrange
        UserProfileDto userProfileDto = new UserProfileDto(testUser);

        // Act
        String token = authService.generateJwtToken(userProfileDto);

        // Assert
        assertNotNull(token);
        assertTrue(token.length() > 0);

        // Verify token structure - would need the key to properly decode, but we can check format
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length); // Header, payload, signature
    }

    @Test
    void isTokenValid_ValidToken() throws Exception {
        // Arrange
        UserProfileDto userProfileDto = new UserProfileDto(testUser);
        String token = authService.generateJwtToken(userProfileDto);

        // Create UserDetails mock
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn(testUser.getEmail());

        // Act
        boolean result = authService.isTokenValid(token, userDetails);

        // Assert
        assertTrue(result);
    }

    @Test
    void isTokenValid_ExpiredToken() throws Exception {
        // Arrange
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn(testUser.getEmail());

        // Create a manually expired token with proper structure but expired date
        String expiredToken = Jwts.builder()
                .setSubject(testUser.getEmail())
                .claim("userId", testUser.getUserId())
                .claim("role", testUser.getRole())
                .setIssuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 30)) // 30 minutes ago
                .setExpiration(new Date(System.currentTimeMillis() - 1000 * 60 * 15)) // 15 minutes ago (expired)
                .signWith(getKeyFromAuthService())
                .compact();

        // Act
        boolean result = authService.isTokenValid(expiredToken, userDetails);

        // Assert
        assertFalse(result);
    }

    // Helper method to get the key from AuthService using reflection
    private Key getKeyFromAuthService() throws Exception {
        Field keyField = AuthServiceImpl.class.getDeclaredField("key");
        keyField.setAccessible(true);
        return (Key) keyField.get(authService);
    }

    @Test
    void extractUsername_Success() {
        // Arrange
        UserProfileDto userProfileDto = new UserProfileDto(testUser);
        String token = authService.generateJwtToken(userProfileDto);

        // Act
        String username = authService.extractUsername(token);

        // Assert
        assertEquals(testUser.getEmail(), username);
    }
}

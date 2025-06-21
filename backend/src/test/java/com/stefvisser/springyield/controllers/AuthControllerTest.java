package com.stefvisser.springyield.controllers;

import com.stefvisser.springyield.dto.*;
import com.stefvisser.springyield.models.User;
import com.stefvisser.springyield.models.UserRole;
import com.stefvisser.springyield.services.AuthService;
import com.stefvisser.springyield.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test class for {@link AuthController}
 *
 * <p>This class contains unit tests for the authentication endpoints in AuthController,
 * focusing on both successful and unsuccessful scenarios for login and signup operations.</p>
 *
 * <p>Key test cases include:
 * <ul>
 *   <li>Successful login with valid credentials</li>
 *   <li>Failed login with invalid credentials</li>
 *   <li>Successful user registration</li>
 *   <li>Failed user registration</li>
 *   <li>Edge cases with null inputs</li>
 * </ul>
 * </p>
 *
 * <p>The tests use Mockito to mock the service layer dependencies ({@link UserService} and {@link AuthService})
 * to isolate the controller logic for unit testing.</p>
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    // Mock dependencies that the controller interacts with
    @Mock
    private UserService userService;

    @Mock
    private AuthService authService;

    // The controller being tested, with mocks injected
    @InjectMocks
    private AuthController authController;

    // Test data objects that will be reused across tests
    private UserLoginDto validLoginDto;
    private UserSignupDto validSignupDto;
    private User testUser;
    private UserProfileDto userProfileDto;
    private String testToken = "test.jwt.token";

    /**
     * Sets up test data before each test method execution.
     *
     * <p>Initializes:
     * <ul>
     *   <li>A valid login DTO with test credentials</li>
     *   <li>A valid signup DTO with test user data</li>
     *   <li>A test User entity with hashed password</li>
     *   <li>A UserProfileDto for the test user</li>
     *   <li>A mock JWT token string</li>
     * </ul>
     * </p>
     */
    @BeforeEach
    void setUp() {
        // Initialize a valid login DTO with test credentials
        validLoginDto = new UserLoginDto("test@springyield.com", "pass");

        // Initialize a valid signup DTO with complete user data
        validSignupDto = new UserSignupDto(
                "Peter",
                "Lusse",
                "pass",
                "p.lusse@springyield.com",
                "+31612345678",
                123456782
        );

        // Create a test user with properly hashed password
        testUser = new User(
                "Peter",
                "Lusse",
                new BCryptPasswordEncoder().encode("pass"), // Simulate password hashing
                "p.lusse@springyield.com",
                123456782,
                "+31612345678",
                UserRole.APPROVED,
                Collections.emptyList() // No accounts for this test user
        );
        testUser.setUserId(1L); // Set a mock user ID

        // Create profile DTO from the test user
        userProfileDto = new UserProfileDto(testUser);
    }

    /**
     * Tests successful login scenario.
     *
     * <p>Verifies that:
     * <ol>
     *   <li>The controller returns HTTP 200 (OK) status</li>
     *   <li>The response contains both token and user profile</li>
     *   <li>The token matches the mock token value</li>
     *   <li>The user profile matches the expected values</li>
     *   <li>The service methods are called exactly once</li>
     * </ol>
     * </p>
     */
    @Test
    void login_withValidCredentials_shouldReturnTokenAndProfile() {
        // Arrange: Configure mock behaviors
        when(userService.login(validLoginDto)).thenReturn(userProfileDto);
        when(authService.generateJwtToken(userProfileDto)).thenReturn(testToken);

        // Act: Call the controller method
        ResponseEntity<AuthResponseDto> response = authController.login(validLoginDto);

        // Assert: Verify response status and content
        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "Successful login should return 200 OK status");

        assertNotNull(response.getBody(),
                "Response body should not be null for successful login");

        assertEquals(testToken, response.getBody().getToken(),
                "Returned token should match the mock token");

        assertEquals(userProfileDto, response.getBody().getUser(),
                "Returned user profile should match the expected profile");

        // Verify mock interactions
        verify(userService, times(1)).login(validLoginDto);
        verify(authService, times(1)).generateJwtToken(userProfileDto);
    }

    /**
     * Tests failed login scenario with invalid credentials.
     *
     * <p>Verifies that:
     * <ol>
     *   <li>The controller returns HTTP 401 (Unauthorized) status</li>
     *   <li>The response body is null</li>
     *   <li>The token generation service is never called</li>
     * </ol>
     * </p>
     */
    @Test
    void login_withInvalidCredentials_shouldReturnUnauthorized() {
        // Arrange: Simulate failed login
        when(userService.login(validLoginDto)).thenReturn(null);

        // Act: Call the controller method
        ResponseEntity<AuthResponseDto> response = authController.login(validLoginDto);

        // Assert: Verify unauthorized response
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(),
                "Failed login should return 401 Unauthorized status");

        assertNull(response.getBody(),
                "Response body should be null for failed login");

        // Verify token generation was not attempted
        verify(authService, never()).generateJwtToken(any());
    }

    /**
     * Tests successful user registration scenario.
     *
     * <p>Verifies that:
     * <ol>
     *   <li>The controller returns HTTP 200 (OK) status</li>
     *   <li>The response contains both token and user profile</li>
     *   <li>The returned email matches the registered user's email</li>
     *   <li>The service methods are called exactly once</li>
     * </ol>
     * </p>
     */
    @Test
    void signup_withValidData_shouldReturnTokenAndProfile() {
        // Arrange: Configure mock behaviors
        when(userService.signupUser(validSignupDto)).thenReturn(testUser);
        UserProfileDto expectedProfileDto = new UserProfileDto(testUser);
        when(authService.generateJwtToken(any(UserProfileDto.class))).thenReturn(testToken);

        // Act: Call the controller method
        ResponseEntity<AuthResponseDto> response = authController.signup(validSignupDto);

        // Assert: Verify response status and content
        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "Successful signup should return 200 OK status");

        assertNotNull(response.getBody(),
                "Response body should not be null for successful signup");

        assertEquals(testToken, response.getBody().getToken(),
                "Returned token should match the mock token");

        assertEquals("p.lusse@springyield.com", response.getBody().getUser().getEmail(),
                "Returned user email should match the registered email");

        // Verify mock interactions
        verify(userService, times(1)).signupUser(validSignupDto);
        verify(authService, times(1)).generateJwtToken(argThat(dto ->
                dto.getEmail().equals(expectedProfileDto.getEmail())
        ));
    }

    /**
     * Tests failed user registration scenario.
     *
     * <p>Verifies that:
     * <ol>
     *   <li>The controller returns HTTP 401 (Unauthorized) status</li>
     *   <li>The response body is null</li>
     *   <li>The token generation service is never called</li>
     * </ol>
     * </p>
     */
    @Test
    void signup_withInvalidData_shouldReturnUnauthorized() {
        // Arrange: Simulate failed signup
        when(userService.signupUser(validSignupDto)).thenReturn(null);

        // Act: Call the controller method
        ResponseEntity<AuthResponseDto> response = authController.signup(validSignupDto);

        // Assert: Verify unauthorized response
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(),
                "Failed signup should return 401 Unauthorized status");

        assertNull(response.getBody(),
                "Response body should be null for failed signup");

        // Verify token generation was not attempted
        verify(authService, never()).generateJwtToken(any());
    }

    /**
     * Tests null input handling for login endpoint.
     *
     * <p>Verifies that:
     * <ol>
     *   <li>The controller throws IllegalArgumentException</li>
     *   <li>No service methods are called</li>
     * </ol>
     * </p>
     */
    @Test
    void login_withNullInput_shouldThrowException() {
        // Act & Assert: Verify exception is thrown
        assertThrows(IllegalArgumentException.class,
                () -> authController.login(null),
                "Null input should throw IllegalArgumentException");

        // Verify no service interactions occurred
        verify(userService, never()).login(any());
        verify(authService, never()).generateJwtToken(any());
    }
}
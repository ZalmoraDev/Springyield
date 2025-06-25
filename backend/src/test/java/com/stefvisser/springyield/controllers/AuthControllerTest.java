package com.stefvisser.springyield.controllers;

import com.stefvisser.springyield.dto.AuthSessionDto;
import com.stefvisser.springyield.dto.UserLoginDto;
import com.stefvisser.springyield.dto.UserProfileDto;
import com.stefvisser.springyield.dto.UserSignupDto;
import com.stefvisser.springyield.models.UserRole;
import com.stefvisser.springyield.services.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private UserLoginDto validLoginDto;
    private UserSignupDto validSignupDto;
    private AuthSessionDto authSessionDto;
    private UserProfileDto userProfileDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

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

        // Setup user profile dto
        userProfileDto = new UserProfileDto();
        userProfileDto.setUserId(1L);
        userProfileDto.setEmail("test@example.com");
        userProfileDto.setFirstName("John");
        userProfileDto.setLastName("Doe");
        userProfileDto.setRole(UserRole.APPROVED);

        // Setup auth session dto
        authSessionDto = new AuthSessionDto("jwt-token-example", userProfileDto);
    }

    @Test
    void login_Success() {
        // Arrange
        when(authService.login(any(UserLoginDto.class))).thenReturn(authSessionDto);

        // Act
        ResponseEntity<?> response = authController.login(validLoginDto);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof AuthSessionDto);
        AuthSessionDto returnedDto = (AuthSessionDto) response.getBody();
        assertEquals("jwt-token-example", returnedDto.getToken());
        assertEquals(userProfileDto.getUserId(), returnedDto.getUser().getUserId());
        verify(authService, times(1)).login(validLoginDto);
    }

    @Test
    void login_InvalidCredentials() {
        // Arrange
        when(authService.login(any(UserLoginDto.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        // Act
        ResponseEntity<?> response = authController.login(validLoginDto);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid email or password", response.getBody());
        verify(authService, times(1)).login(validLoginDto);
    }

    @Test
    void signup_Success() {
        // Arrange
        when(authService.signup(any(UserSignupDto.class))).thenReturn(authSessionDto);

        // Act
        ResponseEntity<?> response = authController.signup(validSignupDto);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof AuthSessionDto);
        AuthSessionDto returnedDto = (AuthSessionDto) response.getBody();
        assertEquals("jwt-token-example", returnedDto.getToken());
        assertEquals(userProfileDto.getUserId(), returnedDto.getUser().getUserId());
        verify(authService, times(1)).signup(validSignupDto);
    }

    @Test
    void signup_EmailAlreadyExists() {
        // Arrange
        when(authService.signup(any(UserSignupDto.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "This email is already registered"));

        // Act
        ResponseEntity<?> response = authController.signup(validSignupDto);

        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("This email is already registered", response.getBody());
        verify(authService, times(1)).signup(validSignupDto);
    }

    @Test
    void signup_MissingRequiredFields() {
        // Arrange
        UserSignupDto invalidSignupDto = new UserSignupDto();
        // Not setting required fields

        when(authService.signup(any(UserSignupDto.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not all required fields are filled in"));

        // Act
        ResponseEntity<?> response = authController.signup(invalidSignupDto);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Not all required fields are filled in", response.getBody());
        verify(authService, times(1)).signup(invalidSignupDto);
    }
}

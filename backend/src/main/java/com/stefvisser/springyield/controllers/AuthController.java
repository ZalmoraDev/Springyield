package com.stefvisser.springyield.controllers;

import com.stefvisser.springyield.dto.AuthResponseDto;
import com.stefvisser.springyield.dto.UserLoginDto;
import com.stefvisser.springyield.dto.UserProfileDto;
import com.stefvisser.springyield.dto.UserSignupDto;
import com.stefvisser.springyield.models.User;
import com.stefvisser.springyield.services.AuthService;
import com.stefvisser.springyield.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for user authentication.
 * <p>
 * This controller handles user authentication operations such as login.
 * It provides endpoints for validating user credentials and generating
 * authentication tokens. All endpoints are mapped to the "/api/auth" base path.
 * </p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    /**
     * Constructs a new AuthController with the required services.
     *
     * @param userService service for user-related operations
     * @param jwtService  service for JWT token generation and validation
     */
    public AuthController(UserService userService, AuthService jwtService) {
        this.userService = userService;
        this.authService = jwtService;
    }

    /**
     * Authenticates a user and generates an authentication token.
     * <p>
     * This endpoint processes user login requests by validating the provided credentials
     * against stored user data. If authentication is successful, it returns a JWT token
     * containing the user ID and role, along with the user's profile information.
     * </p>
     * <p>
     * The authentication flow is:
     * <ol>
     *   <li>Validate the provided credentials against the database</li>
     *   <li>Generate a JWT token containing user ID and role information</li>
     *   <li>Return both the token and user profile for client-side caching</li>
     * </ol>
     * </p>
     *
     * @param loginDto contains the user's login credentials (email and password)
     * @return ResponseEntity containing the authentication token and user profile on success,
     * or an unauthorized status if authentication fails
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody UserLoginDto loginDto) {
        if (loginDto == null) {
            throw new IllegalArgumentException("Login input cannot be null");
        }

        // Uses retrieved form input to validate login
        UserProfileDto userProfile = userService.login(loginDto);

        // If the login is successful, generate a JWT token and return it along with the user profile
        if (userProfile != null) {
            String token = authService.generateJwtToken(userProfile);
            return ResponseEntity.ok(new AuthResponseDto(token, userProfile));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
    }

    /**
     * Registers a new user in the system.
     * <p>
     * This endpoint processes user registration requests by creating a new user account
     * based on the provided signup information. If registration is successful, it returns
     * an authentication token and the user's profile information.
     * </p>
     *
     * @param userSignupDto contains the user registration information
     * @return ResponseEntity containing authentication details on success,
     * or an unauthorized status if registration fails
     */
    @PostMapping("/signup")
    public ResponseEntity<AuthResponseDto> signup(@RequestBody UserSignupDto userSignupDto) {
        try {
            // Validate the user signup data
            if (userSignupDto == null || userSignupDto.getEmail() == null || userSignupDto.getPassword() == null) {
                return ResponseEntity.badRequest().body(null);
            }

            User createdUser = userService.signupUser(userSignupDto);
            if (createdUser != null) {
                UserProfileDto userProfile = new UserProfileDto(createdUser);
                String token = authService.generateJwtToken(userProfile);
                return ResponseEntity.ok(new AuthResponseDto(token, userProfile));
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }
}

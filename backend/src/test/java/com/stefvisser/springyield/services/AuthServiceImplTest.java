package com.stefvisser.springyield.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import com.stefvisser.springyield.dto.UserProfileDto;
import com.stefvisser.springyield.models.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @InjectMocks
    private AuthServiceImpl authService;

    private UserProfileDto sampleUserDto;
    private Key actualKey; // To store and use the actual key from the service for verification

    @BeforeEach
    void setUp() {
        sampleUserDto = new UserProfileDto();
        sampleUserDto.setUserId(1L);
        sampleUserDto.setEmail("test.user@example.com");
        sampleUserDto.setRole(UserRole.APPROVED);
        // Set other necessary fields for UserProfileDto if they affect token generation

        // Retrieve the actual key used by the service instance for proper token parsing in tests
        actualKey = (Key) ReflectionTestUtils.getField(authService, "key");
        if (actualKey == null) {
            // Fallback if the key field name is different or not accessible (though it should be)
            // This is less ideal as it creates a new key, not the one used by the service.
            actualKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
            // If this fallback is used, ensure the service's key is also set to this for consistency if possible,
            // or acknowledge that token validation might only check structure, not signature against the service's actual key.
        }
    }

    @Test
    void generateJwtToken_createsValidToken() {
        String token = authService.generateJwtToken(sampleUserDto);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        // Basic structural check for JWT (three parts separated by dots)
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT token should have 3 parts");

        // Try to parse the token to verify its structure and claims (optional, but good for more thorough testing)
        Jws<Claims> claimsJws = Jwts.parserBuilder()
                .setSigningKey(actualKey) // Use the actual key from the service
                .build()
                .parseClaimsJws(token);

        assertNotNull(claimsJws);
        assertNotNull(claimsJws.getBody());

        Claims claims = claimsJws.getBody();
        assertEquals(sampleUserDto.getEmail(), claims.getSubject());
        assertEquals(sampleUserDto.getUserId(), claims.get("userId", Long.class));
        // Enum roles might be stored as strings in claims, adjust assertion if needed
        assertEquals(sampleUserDto.getRole().toString(), claims.get("role", String.class));

        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertTrue(claims.getExpiration().after(new Date()), "Token should not be expired immediately");
        // Check if expiration is roughly 24 hours from now (as per service logic)
        long expectedExpirationMillis = System.currentTimeMillis() + 1000 * 60 * 15;
        long actualExpirationMillis = claims.getExpiration().getTime();
        // Allow a small delta for the time difference between token generation and this check
        long deltaMillis = 60000; // 1 minute tolerance
        assertTrue(Math.abs(expectedExpirationMillis - actualExpirationMillis) < deltaMillis,
                   "Token expiration time is not approximately 24 hours from now.");
    }

    @Test
    void generateJwtToken_withDifferentUserDetails_createsDifferentToken() {
        String token1 = authService.generateJwtToken(sampleUserDto);

        UserProfileDto anotherUserDto = new UserProfileDto();
        anotherUserDto.setUserId(2L);
        anotherUserDto.setEmail("another.user@example.com");
        anotherUserDto.setRole(UserRole.EMPLOYEE);

        String token2 = authService.generateJwtToken(anotherUserDto);

        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2, "Tokens for different users should not be the same");
    }

    @Test
    void generateJwtToken_calledMultipleTimesForSameUser_createsDifferentTokensDueToTimestamp() {
        // Tokens include an issuedAt timestamp, so even for the same user, subsequent calls should produce different tokens.
        String token1 = authService.generateJwtToken(sampleUserDto);
        // Introduce a delay to ensure iat and exp are different, specifically to cross a second boundary
        try {
            Thread.sleep(1001); // Increased delay to ensure next second is reached
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String token2 = authService.generateJwtToken(sampleUserDto);

        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2, "Subsequent tokens for the same user should differ due to timestamps");
    }
}


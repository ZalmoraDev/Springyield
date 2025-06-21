package com.stefvisser.springyield.services;

import com.stefvisser.springyield.dto.UserProfileDto;
import org.springframework.security.core.userdetails.UserDetails;

public interface AuthService {
    String generateJwtToken(UserProfileDto user);

    // Extract username from JWT token
    String extractUsername(String token);

    // Validate if token belongs to given user and is still valid
    boolean isTokenValid(String token, UserDetails userDetails);
}

package com.stefvisser.springyield.services;

import com.stefvisser.springyield.dto.AuthSessionDto;
import com.stefvisser.springyield.dto.UserLoginDto;
import com.stefvisser.springyield.dto.UserProfileDto;
import com.stefvisser.springyield.dto.UserSignupDto;
import org.springframework.security.core.userdetails.UserDetails;

public interface AuthService {
    AuthSessionDto signup(UserSignupDto dto);
    AuthSessionDto login(UserLoginDto loginDto);

    String generateJwtToken(UserProfileDto user);

    // Extract username from JWT token
    String extractUsername(String token);

    // Validate if token belongs to given user and is still valid
    boolean isTokenValid(String token, UserDetails userDetails);
}


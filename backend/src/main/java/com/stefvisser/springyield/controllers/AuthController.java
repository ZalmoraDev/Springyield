package com.stefvisser.springyield.controllers;

import com.stefvisser.springyield.dto.AuthSessionDto;
import com.stefvisser.springyield.dto.UserLoginDto;
import com.stefvisser.springyield.dto.UserSignupDto;
import com.stefvisser.springyield.services.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLoginDto loginDto) {
        try {
            AuthSessionDto response = authService.login(loginDto);
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody UserSignupDto signupDto) {
        try {
            AuthSessionDto response = authService.signup(signupDto);
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }
}

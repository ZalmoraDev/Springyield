package com.stefvisser.springyield.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.stefvisser.springyield.models.UserRole;

/**
 * Data Transfer Object for updating user information.
 * <p>
 * Includes all User fields, except for accounts and userId.
 * </p>
 */
@Data
@NoArgsConstructor
public class UserUpdateDto {
    private String firstName;
    private String lastName;
    private String email;
    private Integer bsnNumber; // Changed to Integer to allow null if not provided, though typically BSN is int
    private String phoneNumber;
    private UserRole role;
    private String password; // Password is optional for updates, can be null if not changing
}


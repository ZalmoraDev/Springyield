package com.stefvisser.springyield.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for user authentication requests.
 * <p>
 * This class encapsulates the credentials required for a user login attempt.
 * It serves as the contract between client applications and the backend
 * authentication service, containing only the essential information needed
 * for user authentication.
 * </p>
 *
 * @since 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserLoginDto {
    /**
     * The user's email address, which serves as their username for authentication.
     */
    private String email;

    /**
     * The user's password for authentication.
     * <p>
     * This password is transmitted in plain text within the DTO but should
     * be handled securely during transport (e.g., over HTTPS) and compared
     * against a hashed version stored in the database.
     * </p>
     */
    private String password;
}

package com.stefvisser.springyield.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for user registration requests.
 * <p>
 * This class captures all required information from a user during the signup process.
 * It contains personal information, contact details, and can include associated account
 * profiles. This DTO serves as the contract between client applications and the backend
 * for new user registration.
 * </p>
 *
 * @since 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSignupDto {
    /**
     * The user's first name and last name.
     */
    private String firstName, lastName;

    /**
     * The plain-text password chosen by the user during registration.
     * <p>
     * Note: This password is stored as plain text in this DTO but will be
     * encrypted before storage in the database.
     * </p>
     */
    private String password;

    /**
     * The user's email address, which will serve as their username for authentication.
     */
    private String email;

    /**
     * The user's contact phone number.
     */
    private String phoneNumber;

    /**
     * The user's BSN (Burgerservicenummer) - Dutch citizen service number.
     * <p>
     * This is a unique identifier assigned to Dutch residents.
     * </p>
     */
    private int bsnNumber;

    /**
     * List of bank accounts associated with this user during registration.
     */
    private List<AccountProfileDto> accounts;

    /**
     * Flag indicating whether the user has been approved by an administrator.
     * <p>
     * By default, new users are not approved (false).
     * </p>
     */
    private Boolean approvedUser = false;

    /**
     * Constructs a new UserSignupDto with the specified user information.
     *
     * @param firstName The user's first name
     * @param lastName The user's last name
     * @param password The user's chosen password (plain-text)
     * @param email The user's email address
     * @param phoneNumber The user's phone number
     * @param bsnNumber The user's BSN number
     */
    public UserSignupDto(String firstName, String lastName, String password, String email, String phoneNumber, int bsnNumber) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password; // Here the password is plaintext, which will be used in User creation with BCryptPasswordEncoder
        this.email = email;
        this.bsnNumber = bsnNumber;
        this.phoneNumber = phoneNumber;
        this.accounts = new ArrayList<>();
    }
}


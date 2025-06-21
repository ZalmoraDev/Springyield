package com.stefvisser.springyield.dto;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.stefvisser.springyield.models.User;
import com.stefvisser.springyield.models.UserRole;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for user profile information.
 * <p>
 * This class provides a streamlined view of user data for transferring between
 * service layers and client applications. It contains essential personal information,
 * contact details, role information, and a summarized list of associated accounts.
 * It excludes sensitive data like passwords and includes only the data needed for
 * profile display and management.
 * </p>
 */
@Data
@NoArgsConstructor
public class UserProfileDto {
    /**
     * The unique identifier for the user.
     */
    private Long userId;

    /**
     * The user's first name.
     */
    private String firstName;

    /**
     * The user's last name.
     */
    private String lastName;

    /**
     * The user's email address, typically used as their username for authentication.
     */
    private String email;

    /**
     * The user's BSN (Burgerservicenummer) - Dutch citizen service number.
     * <p>
     * This is a unique identifier assigned to Dutch residents.
     * </p>
     */
    private int bsnNumber;

    /**
     * The user's contact phone number.
     */
    private String phoneNumber;

    /**
     * The user's role in the system (e.g., CUSTOMER, EMPLOYEE).
     * <p>
     * This determines the user's access levels and available functionality.
     * </p>
     */
    private UserRole role;

    /**
     * List of simplified account profiles associated with this user.
     * <p>
     * These are lightweight representations of the user's accounts containing
     * only essential information needed for display purposes.
     * </p>
     */
    @JsonManagedReference
    private List<AccountProfileDto> accounts = new ArrayList<>();

    /**
     * Constructs a UserProfileDto from a User entity.
     * <p>
     * This constructor extracts relevant profile information from the User entity
     * and converts associated accounts to lightweight AccountProfileDto objects.
     * </p>
     *
     * @param user The User entity to convert to a profile DTO
     */
    public UserProfileDto(User user) {
        this.userId = user.getUserId();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.email = user.getEmail();
        this.bsnNumber = user.getBsnNumber();
        this.phoneNumber = user.getPhoneNumber();
        this.role = user.getRole();

        ///  Instead of loading full Account objects, we only load the minimal data needed for the frontend
        ///  Later, we can POST-request more data depending on the UI-page & party trying to access it (Customer / Employee)
        this.accounts = user.getAccounts().stream()
                .map(AccountProfileDto::wrap)
                .toList();
    }

    public UserProfileDto withoutAccounts(User user) {
        this.userId = user.getUserId();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.email = user.getEmail();
        this.bsnNumber = user.getBsnNumber();
        this.phoneNumber = user.getPhoneNumber();
        this.role = user.getRole();
        return this;
    }

    /**
     * Static factory method to create a UserProfileDto from a User entity.
     * <p>
     * This convenience method provides a cleaner way to convert User entities to DTOs.
     * </p>
     *
     * @param user The User entity to wrap
     * @return A new UserProfileDto containing the user's profile information
     */
    public static UserProfileDto wrap(User user) {
        return new UserProfileDto(user);
    }

    /**
     * Wrapper without using the accounts field.
     * <p>
     *  Removes the accounts field from the DTO to prevent loading account data.
     * </p>
     */
    public static UserProfileDto wrapWithoutAccount(User user) {
        return new UserProfileDto().withoutAccounts(user);
    }
}


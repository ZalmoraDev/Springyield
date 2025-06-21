package com.stefvisser.springyield.dto;

import lombok.Getter;

/**
 * Data Transfer Object for authentication responses.
 * <p>
 * This class encapsulates the data returned to a client after a successful
 * authentication attempt. It includes the JWT authentication token needed for
 * subsequent authorized requests, as well as the authenticated user's profile
 * information for immediate use in the client application.
 * </p>
 *
 * @since 1.0
 */
@Getter
public class AuthResponseDto {
    /**
     * The JWT authentication token.
     * <p>
     * This token should be included in subsequent API requests in the
     * Authorization header to authenticate the user.
     * </p>
     */
    private String token;

    /**
     * The authenticated user's profile information.
     * <p>
     * This provides immediate access to user details without requiring
     * an additional API call after authentication.
     * </p>
     */
    private UserProfileDto user;

    /**
     * Constructs an AuthResponseDto with the specified token and user profile.
     *
     * @param token The JWT authentication token for the authenticated user
     * @param user The profile information of the authenticated user
     */
    public AuthResponseDto(String token, UserProfileDto user) {
        this.token = token;
        this.user = user;
    }
}

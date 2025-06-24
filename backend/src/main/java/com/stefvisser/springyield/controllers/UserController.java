package com.stefvisser.springyield.controllers;

import com.stefvisser.springyield.dto.*;
import com.stefvisser.springyield.models.User;
import com.stefvisser.springyield.models.UserRole;
import com.stefvisser.springyield.services.AuthService;
import com.stefvisser.springyield.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST controller for managing user accounts.
 * <p>
 * This controller provides endpoints for user management, including registration,
 * searching for users, and managing user approval status. It handles HTTP requests
 * related to user operations and delegates the business logic to the appropriate
 * service classes. All endpoints are mapped to the "/api/user" base path.
 * </p>
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    /**
     * Constructs a new UserController with the required services.
     *
     * @param userService service for user-related operations
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Retrieves a user profile by its unique ID.
     * <p>
     * Allows fetching the details of a specific user by their ID.
     * It requires the authenticated user's credentials to ensure proper access control.
     * Also requires execUser to be an employee or the user themselves to retrieve their own profile.
     * </p>
     *
     * @param execUser the authenticated user performing the request
     * @param targetUserId the unique identifier of the target user to retrieve
     * @return ResponseEntity containing the user profile data
     */
    @GetMapping("/{targetUserId}")
    public ResponseEntity<?> getUserById(@AuthenticationPrincipal User execUser, @PathVariable Long targetUserId) {
        try {
            User targetUser = userService.getUserById(execUser, targetUserId);
            return ResponseEntity.ok(new UserProfileDto(targetUser));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    /**
     * Searches for users based on specified criteria with pagination.
     * <p>
     * This endpoint allows filtering users by a search query and role,
     * with results returned in a paginated format for efficient data transfer.
     * </p>
     *
     * @param execUser the authenticated user performing the request
     * @param query optional search string to filter users by name or other attributes
     * @param role optional role filter (e.g., CUSTOMER, EMPLOYEE)
     * @param limit maximum number of results per page (defaults to 10)
     * @param offset starting position for pagination (defaults to 0)
     * @return ResponseEntity containing paginated execUser search results
     */
    @GetMapping("/search")
    public ResponseEntity<?> search(
            @AuthenticationPrincipal User execUser,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) int limit,
            @RequestParam(required = false) int offset)
    {
        try {
            PaginatedDataDto<UserProfileDto> paginatedUsers = userService.search(execUser, query, role, limit, offset);
            return ResponseEntity.ok(paginatedUsers);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getBody());
        }
    }

    /**
     * Approves a execUser account, enabling full system access.
     * <p>
     * This endpoint changes a execUser's approval status to approved,
     * which typically enables them to perform banking operations.
     * This action is generally restricted to administrative users.
     * </p>
     *
     * @param execUser the authenticated user performing the request
     * @param userId the unique identifier of the execUser to approve
     * @return ResponseEntity containing the updated execUser profile
     */
    @PutMapping("/{userId}/approve")
    public ResponseEntity<?> approveUser(@AuthenticationPrincipal User execUser, @PathVariable Long userId, @RequestBody UserApprovalDto approvalDTO) {
        try {
            userService.approveUser(execUser, userId, approvalDTO.getDailyLimit(), approvalDTO.getAbsoluteLimit());
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    /**
     * Updates an existing execUser's details.
     * <p>
     * This endpoint allows updating mutable fields of a execUser's profile.
     * It requires the execUser ID in the path and the updated data in the request body.
     * </p>
     *
     * @param execUser the authenticated user performing the update
     * @param targetUserId the unique identifier of the target user to update
     * @param userUpdateDto DTO containing the fields from the frontend to update th db with
     * @return ResponseEntity containing the updated execUser profile
     */
    @PutMapping("/{targetUserId}/update")
    public ResponseEntity<?> updateUser(@AuthenticationPrincipal User execUser, @PathVariable Long targetUserId, @RequestBody UserUpdateDto userUpdateDto) {
        try {
            userService.updateUser(execUser, targetUserId, userUpdateDto);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    /**
     * Deletes a user account.
     * <p>
     * This endpoint allows an authenticated user to delete their own account
     * or an employee to delete any user account. It prevents employees from
     * deleting their own accounts.
     * </p>
     *
     * @param execUser the authenticated user performing the deletion
     * @param targetUserId the unique identifier of the user to delete
     * @return ResponseEntity indicating the result of the deletion operation
     */
    @PostMapping("/{targetUserId}/delete")
    public ResponseEntity<?> deleteUser(@AuthenticationPrincipal User execUser, @PathVariable Long targetUserId) {
        try {
            userService.deleteUser(execUser, targetUserId);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }
}

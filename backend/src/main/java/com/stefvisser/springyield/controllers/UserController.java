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
    private final AuthService authService;

    /**
     * Constructs a new UserController with the required services.
     *
     * @param userService service for user-related operations
     * @param authService service for authentication and token management
     */
    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    /**
     * Searches for users based on specified criteria with pagination.
     * <p>
     * This endpoint allows filtering users by a search query and role,
     * with results returned in a paginated format for efficient data transfer.
     * </p>
     *
     * @param query optional search string to filter users by name or other attributes
     * @param role optional role filter (e.g., CUSTOMER, EMPLOYEE)
     * @param limit maximum number of results per page (defaults to 10)
     * @param offset starting position for pagination (defaults to 0)
     * @return ResponseEntity containing paginated user search results
     */
    @GetMapping("/search")
    public ResponseEntity<?> getUserByName(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) int offset)
    {
        try {
            if (user == null)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");

            User executingUser = userService.getUserById(user.getUserId());

            if (!executingUser.isEmployee())
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to search users");


            if (limit == null) limit = 10;
            if (offset < 0) offset = 0;
            if (query == null) query = "";
            boolean isAdmin = executingUser.getRole() == UserRole.ADMIN;


            PaginatedDataDTO<UserProfileDto> paginatedUsers = userService.search(query, role, limit, offset, isAdmin);

            return ResponseEntity.ok(paginatedUsers);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getBody());
        }
    }

    /**
     * Approves a user account, enabling full system access.
     * <p>
     * This endpoint changes a user's approval status to approved,
     * which typically enables them to perform banking operations.
     * This action is generally restricted to administrative users.
     * </p>
     *
     * @param userId the unique identifier of the user to approve
     * @return ResponseEntity containing the updated user profile
     */
    @PutMapping("/{userId}/approve")
    public ResponseEntity<?> approveUser(@AuthenticationPrincipal User user, @PathVariable Long userId, @RequestBody UserApprovalDTO approvalDTO) {
        try {
            if (user == null)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");

            User executingUser = userService.getUserById(user.getUserId());

            if (executingUser == null || !executingUser.isEmployee()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to approve users");
            }

            UserProfileDto result = userService.approveUser(userId, approvalDTO.getDailyLimit(), approvalDTO.getAbsoluteLimit());
            return ResponseEntity.ok(result);

        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getBody());
        }

    }

    /**
     * Updates an existing user's details.
     * <p>
     * This endpoint allows updating mutable fields of a user's profile.
     * It requires the user ID in the path and the updated data in the request body.
     * </p>
     *
     * @param userId the unique identifier of the user to update
     * @param userUpdateDto DTO containing the fields to update
     * @return ResponseEntity containing the updated user profile
     */
    @PutMapping("/{userId}/update")
    public ResponseEntity<?> updateUser(@AuthenticationPrincipal User user, @PathVariable Long userId, @RequestBody UserUpdateDto userUpdateDto) {
        try {
            if (user == null)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");

            User executingUser = userService.getUserById(user.getUserId());

            // Check if user is updating their own profile or is an employee
            if (executingUser == null || (!executingUser.getUserId().equals(userId) && !executingUser.isEmployee())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to update this user");
            }

            // Validate required fields
            if (userUpdateDto.getFirstName() == null || userUpdateDto.getLastName() == null ||
                    userUpdateDto.getEmail() == null || userUpdateDto.getPhoneNumber() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required fields cannot be empty");
            }

            // Role change check
            if (userUpdateDto.getRole() != null && !executingUser.getRole().equals(UserRole.ADMIN)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can change user roles");
            }

            UserProfileDto updatedUser = userService.updateUser(userId, userUpdateDto);
            return ResponseEntity.ok(updatedUser);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getBody());
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserById(@AuthenticationPrincipal User user, @PathVariable Long userId) {
        try {
            if (user == null)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");

            User executingUser = userService.getUserById(user.getUserId());

            if (!executingUser.getUserId().equals(user.getUserId()) && !executingUser.isEmployee())
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to view this user's profile");


            User foundUser = userService.getUserById(userId);

            if (foundUser == null) {
                return ResponseEntity.notFound().build();
            }

            if (foundUser.isEmployee() && !executingUser.getUserId().equals(foundUser.getUserId())) {
                if (executingUser.getRole() != UserRole.ADMIN) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to view employee profiles");
                }
            }


            return ResponseEntity.ok(new UserProfileDto(foundUser));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getBody());
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
     * @param userId the unique identifier of the user to delete
     * @return ResponseEntity indicating the result of the deletion operation
     */
    @DeleteMapping("/{userId}/delete")
    public ResponseEntity<?> deleteUser(@AuthenticationPrincipal User user, @PathVariable Long userId) {
        try {
            if (user == null)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");

            User executingUser = userService.getUserById(user.getUserId());

            if (executingUser == null || (!executingUser.getUserId().equals(userId) && !user.isEmployee())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to delete this user");
            }

            if (executingUser.getUserId().equals(userId) && executingUser.isEmployee()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Employees cannot delete their own accounts");
            }


            userService.deleteUser(userId);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getBody());
        }
    }

}

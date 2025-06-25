package com.stefvisser.springyield.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.transaction.Transactional;
import com.stefvisser.springyield.dto.*;
import com.stefvisser.springyield.models.*;
import com.stefvisser.springyield.repositories.AccountRepository;
import com.stefvisser.springyield.repositories.UserRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


@Primary
@Service
class UserServiceImpl implements UserService {
    private final AccountService accountService;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserServiceImpl(AccountService accountService, UserRepository userRepository, AccountRepository accountRepository) {
        this.accountService = accountService;
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    // -----------------------------------------------------------------------------------------------------------------
    // API Methods
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Retrieves a user profile by its unique ID.
     * <p>
     * Allows fetching the details of a specific user by their ID.
     * It requires the authenticated user's credentials to ensure proper access control.
     * Also requires execUser to be an employee or the user themselves to retrieve their own profile.
     * </p>
     *
     * @param execUser     the authenticated user performing the request
     * @param targetUserId the unique identifier of the target user to retrieve
     * @return User object containing the requested user's profile information
     */
    public User getUserById(User execUser, Long targetUserId) {
        if (execUser == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");

        if (!execUser.getUserId().equals(targetUserId) && !execUser.isEmployee())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to view this user's profile");

        User targetUser = userRepository.findByUserId(targetUserId);
        if (targetUser == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + targetUserId);

        // Check if execUser is trying to view their own profile, then allow it
        // Restrict employees from viewing other employee profiles unless they are admins
        if (!execUser.getUserId().equals(targetUser.getUserId()))
            if (targetUser.getRole() == UserRole.EMPLOYEE || targetUser.getRole() == UserRole.ADMIN)
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to view employee/admin profiles");

        return targetUser;
    }

    /**
     * Searches for users based on specified criteria with pagination.
     * <p>
     * This endpoint allows filtering users by a search query and role,
     * with results returned in a paginated format for efficient data transfer.
     * </p>
     *
     * @param execUser the authenticated user performing the request
     * @param query    optional search string to filter users by name or other attributes
     * @param role     optional role filter (e.g., APPROVED, EMPLOYEE)
     * @param limit    maximum number of results per page (defaults to 10)
     * @param offset   starting position for pagination (defaults to 0)
     * @return PaginatedDataDto containing paginated user search results
     */
    public PaginatedDataDto<UserProfileDto> search(User execUser, String query, UserRole role, int limit, int offset) {
        if (execUser == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");

        if (!execUser.isEmployee())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to search users");

        if (limit <= 0) limit = 10;  // Check if limit is zero or negative
        if (offset < 0) offset = 0;
        if (query == null) query = "";
        boolean isAdmin = execUser.getRole() == UserRole.ADMIN;

        return userRepository.search(query, role, limit, offset, isAdmin);
    }

    /**
     * Approves a user by changing their role to APPROVED and creating normal and savings accounts for them.
     *
     * @param execUser     the authenticated user performing the request
     * @param targetUserId the ID of the user to approve
     * @return UserProfileDto containing the updated user information
     * @ Transactional ensures that the operation is atomic, meaning either all changes are applied or none.
     */
    @Transactional
    public void approveUser(User execUser, Long targetUserId, BigDecimal dailyLimit, BigDecimal absoluteLimit, BigDecimal balanceLimit) {
        if (execUser == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");

        if (!execUser.isEmployee())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to approve users");

        User targetUser = userRepository.findByUserId(targetUserId);
        if (targetUser == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + targetUserId);

        if (targetUser.getRole() != UserRole.UNAPPROVED)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is already approved");

        // Change role to APPROVED
        targetUser.setRole(UserRole.APPROVED);
        userRepository.save(targetUser);

        // Create accounts
        BigDecimal initialBalance = BigDecimal.ZERO;

        // Create normal account and savings account to the database
        Account savedNormalAccount = accountService.createAccount(
                targetUser, AccountType.PAYMENT,
                dailyLimit, absoluteLimit,
                initialBalance, balanceLimit);

        Account savedSavingsAccount = accountService.createAccount(
                targetUser, AccountType.SAVINGS,
                dailyLimit, absoluteLimit,
                initialBalance, balanceLimit);

        // Assign created accounts to the user
        targetUser.getAccounts().add(savedNormalAccount);
        targetUser.getAccounts().add(savedSavingsAccount);
    }

    /**
     * Updates an existing execUser's details.
     * <p>
     * This endpoint allows updating mutable fields of a execUser's profile.
     * It requires the execUser ID in the path and the updated data in the request body.
     * </p>
     *
     * @param execUser      the authenticated user performing the update
     * @param targetUserId  the unique identifier of the target user to update
     * @param userUpdateDto DTO containing the fields from the frontend to update th db with
     * @return ResponseEntity containing the updated execUser profile
     */
    
    @Transactional
    public UserProfileDto updateUser(User execUser, Long targetUserId, UserUpdateDto userUpdateDto) {
        // Get the authenticated user (the one performing the update)
        if (execUser == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");

        // Check if target user exists
        User targetUser = userRepository.findByUserId(targetUserId);
        if (targetUser == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + targetUserId);

        // Check if execUser is updating their own profile or is an employee, restrict customers from updating other users
        if (!execUser.getUserId().equals(targetUserId) && !execUser.isEmployee())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to update this user");

        // Role change check
        if (userUpdateDto.getRole() != null && !execUser.getRole().equals(UserRole.ADMIN))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can change execUser roles");

        // Check for email uniqueness if email is being changed
        if (userUpdateDto.getEmail() != null && !userUpdateDto.getEmail().equals(targetUser.getEmail())) {
            User existingUser = userRepository.findByEmail(userUpdateDto.getEmail());
            if (existingUser != null && !existingUser.getUserId().equals(targetUserId))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }

        // Prevents empty fields from overwriting existing values
        if (userUpdateDto.getFirstName() != null) targetUser.setFirstName(userUpdateDto.getFirstName());
        if (userUpdateDto.getLastName() != null) targetUser.setLastName(userUpdateDto.getLastName());
        if (userUpdateDto.getEmail() != null) targetUser.setEmail(userUpdateDto.getEmail());
        if (userUpdateDto.getBsnNumber() != null) targetUser.setBsnNumber(userUpdateDto.getBsnNumber());
        if (userUpdateDto.getPhoneNumber() != null) targetUser.setPhoneNumber(userUpdateDto.getPhoneNumber());
        if (userUpdateDto.getRole() != null) targetUser.setRole(userUpdateDto.getRole());
        if (userUpdateDto.getPassword() != null)
            targetUser.setPassword(passwordEncoder.encode(userUpdateDto.getPassword().trim()));

        return new UserProfileDto(userRepository.save(targetUser));
    }

    /**
     * Deletes a user by their ID.
     * <p>
     * This method allows an authenticated user to delete another user's profile.
     * It requires the authenticated user to be an employee or the user themselves to delete their own profile.
     * </p>
     *
     * @param execUser      the authenticated user performing the deletion
     * @param targetUserId  the unique identifier of the target user to delete
     */
    
    @Transactional
    public void deleteUser(User execUser, Long targetUserId) {
        // Get the authenticated user (the one performing the deletion)
        if (execUser == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");

        // Get the target user (the one being deleted)
        User targetUser = this.getUserById(execUser, targetUserId);
        if (targetUser == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");

        // Check if execUser is deleting their own profile or is an employee. Restrict customers from deleting other users
        if (!execUser.getUserId().equals(targetUserId) && !execUser.isEmployee())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to delete this user");

        // Prevent employees from deleting their own accounts
        if (execUser.getUserId().equals(targetUserId) && execUser.isEmployee())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Employees cannot delete their own accounts");

        // Detach all accounts from the target user before deletion and mark them as inactive (Soft Delete)
        if (targetUser.getAccounts() != null && !targetUser.getAccounts().isEmpty()) {
            // Create a copy of the accounts list to avoid ConcurrentModificationException
            List<Account> accountsToUpdate = new ArrayList<>(targetUser.getAccounts());
            targetUser.getAccounts().clear();

            for (Account account : accountsToUpdate) {
                account.setStatus(AccountStatus.DEACTIVATED); // Mark account as deactivated
                account.setUser(null); // Detach from user
            }

            // Save the updated accounts
            accountService.saveAll(accountsToUpdate);
        }

        // Now delete the user
        userRepository.delete(targetUser);
    }


    // -----------------------------------------------------------------------------------------------------------------
    // Non-API Methods (Less authentication required, since they are used internally)
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Finds a user by their email address.
     * <p>
     * This method is used internally to retrieve a user entity based on their email.
     * </p>
     *
     * @param email the email address of the user to find
     * @return User entity if found, otherwise null
     */
    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Saves a user to the database.
     * <p>
     * This method is used internally to save a single user entity.
     * </p>
     *
     * @param user the User entity to save
     */
    public void save(User user) {
        userRepository.save(user);
    }

    /**
     * Saves a list of users to the database.
     * <p>
     * This method is used internally to save multiple user entities at once.
     * </p>
     *
     * @param users the list of User entities to save
     */
    public void saveAll(List<User> users) {
        userRepository.saveAll(users);
    }
}

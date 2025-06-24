package com.stefvisser.springyield.controllers;

import com.stefvisser.springyield.dto.*;
import com.stefvisser.springyield.models.*;
import com.stefvisser.springyield.services.AccountService;
import com.stefvisser.springyield.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

/**
 * REST controller for managing bank accounts.
 * <p>
 * This controller provides endpoints for retrieving, searching, and managing bank accounts.
 * It handles HTTP requests related to account operations and delegates the business logic
 * to the appropriate service classes. All endpoints are mapped to the "/api/account" base path.
 * </p>
 */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;
    private final UserService userService;

    /**
     * Constructs a new AccountController with the required services.
     *
     * @param accountService service for account-related operations
     * @param userService service for user-related operations
     */
    public AccountController(AccountService accountService, UserService userService) {
        this.accountService = accountService;
        this.userService = userService;
    }

    /**
     * Searches for accounts based on specified criteria with pagination.
     * <p>
     * This endpoint allows filtering accounts by a search query and account type,
     * with results returned in a paginated format for efficient data transfer.
     * </p>
     *
     * @param query optional search string to filter accounts by various attributes
     * @param accountType optional account type filter (e.g., CURRENT, SAVINGS)
     * @param limit maximum number of results per page (defaults to 10)
     * @param offset starting position for pagination (defaults to 0)
     * @return ResponseEntity containing paginated account search results
     */
    @GetMapping("/search")
    public ResponseEntity<?> getAccountsBySearch(@AuthenticationPrincipal User execUser, @RequestParam(required = false) String query, @RequestParam(required = false) AccountType accountType, @RequestParam(required = false) AccountStatus status, @RequestParam(required = false) Integer limit, @RequestParam(required = false) int offset) {
        try {
            if (execUser == null)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");

            if (!execUser.isEmployee())
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to view accounts");


            if (limit == null) limit = 10;
            if (offset < 0) offset = 0;
            if (query == null) query = "";

            PaginatedDataDto<AccountProfileDto> accounts = accountService.searchAccount(query, accountType, status, limit, offset);
            return ResponseEntity.ok(accounts);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getBody());
        }
    }

    @PutMapping("/{accountId}/limits")
    public ResponseEntity<?> updateAccountLimits(
            @AuthenticationPrincipal User execUser,
            @PathVariable Long accountId,
            @RequestBody AccountLimitsDto limitsDTO) {
        try {
            if (execUser == null)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");

            if (!execUser.isEmployee()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to update account limits");
            }

            Account updatedAccount = accountService.updateAccountLimits(
                    accountId,
                    limitsDTO.getDailyLimit(),
                    limitsDTO.getAbsoluteLimit()
            );

            return ResponseEntity.ok(AccountProfileDto.wrap(updatedAccount));

        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getBody());
        }
    }

    @GetMapping("/addressbook/search")
    public ResponseEntity<List<AddressBookEntryDto>> searchAccountsByName(@RequestParam(required = false) String query) {
        PaginatedDataDto<AccountProfileDto> accounts = accountService.searchAccountByName(query);

        if (accounts == null || accounts.getData().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<AddressBookEntryDto> addressBookEntries = accounts.getData().stream()
                .map(AddressBookEntryDto::wrap)
                .toList();

        return ResponseEntity.ok(addressBookEntries);
    }

    /**
     * Retrieves a execUser's profile along with their associated accounts.
     * <p>
     * This endpoint returns combined data about a execUser and their bank accounts,
     * providing a comprehensive view of a customer's profile and financial accounts.
     * </p>
     *
     * @param targetUserId the unique identifier of the execUser
     * @return ResponseEntity containing a map with the execUser profile and their accounts
     */
    @GetMapping("/{targetUserId}")
    public ResponseEntity<?> getUserAndAccounts(@AuthenticationPrincipal User execUser, @PathVariable Long targetUserId) {
        try {
            if (execUser == null)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");

            if (!execUser.isEmployee())
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to view this execUser's profile");

            User targetUser = userService.getUserById(execUser, targetUserId);

            if (targetUser == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(new UserProfileDto(targetUser));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getBody());
        }
    }

    @GetMapping("/iban/{iban}")
    public ResponseEntity<?> getAccountByIban(@AuthenticationPrincipal User execUser, @PathVariable String iban) {
        try {
            if (execUser == null)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");

            Account account = accountService.getAccountByIban(iban);

            if (account == null) {
                return ResponseEntity.notFound().build();
            }

            if (!Objects.equals(account.getUser().getUserId(), execUser.getUserId()) && !execUser.isEmployee()){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to view this account");
            }

            return ResponseEntity.ok(new AccountProfileDto(account));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getBody());
        }
    }
}

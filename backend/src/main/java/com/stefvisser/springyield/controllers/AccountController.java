package com.stefvisser.springyield.controllers;

import com.stefvisser.springyield.dto.*;
import com.stefvisser.springyield.models.*;
import com.stefvisser.springyield.services.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Searches for accounts based on specified criteria with pagination.
     *
     * @param execUser the user executing the request
     * @param query optional search string to filter accounts by various attributes
     * @param accountType optional account type filter (e.g., CURRENT, SAVINGS)
     * @param status optional account status filter (e.g., ACTIVE, CLOSED)
     * @param limit maximum number of results per page (defaults to 10)
     * @param offset starting position for pagination (defaults to 0)
     * @return ResponseEntity containing paginated account search results
     */
    @GetMapping("/search")
    public ResponseEntity<?> search(@AuthenticationPrincipal User execUser, @RequestParam(required = false) String query, @RequestParam(required = false) AccountType accountType, @RequestParam(required = false) AccountStatus status, @RequestParam(required = false) Integer limit, @RequestParam(required = false) int offset) {
        try {
            PaginatedDataDto<AccountProfileDto> accounts = accountService.search(execUser, query, accountType, status, limit, offset);
            return ResponseEntity.ok(accounts);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    /**
     * Retrieves an account by its IBAN.
     *
     * @param execUser the user executing the request
     * @param iban the IBAN of the account to retrieve
     * @return ResponseEntity with the account details or error message
     */
    @GetMapping("/iban/{iban}")
    public ResponseEntity<?> getAccountByIban(@AuthenticationPrincipal User execUser, @PathVariable String iban) {
        try {
            Account account = accountService.getAccountByIban(execUser, iban);
            return ResponseEntity.ok().body(account);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    /**
     * Updates the balance limits for a specific account.
     *
     * @param execUser the user executing the request
     * @param accountId the ID of the account to update
     * @param limitsDTO the DTO containing the new balance limits
     * @return ResponseEntity with updated account profile or error message
     */
    @PutMapping("/{accountId}/limits")
    public ResponseEntity<?> updateBalanceLimits(
            @AuthenticationPrincipal User execUser,
            @PathVariable Long accountId,
            @RequestBody AccountLimitsDto limitsDTO) {
        try {
            Account updatedAccount = accountService.updateBalanceLimits(
                    execUser, accountId,
                    limitsDTO.getDailyLimit(),
                    limitsDTO.getAbsoluteLimit()
            );
            return ResponseEntity.ok(AccountProfileDto.wrap(updatedAccount));

        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }
}

package com.stefvisser.springyield.dto;

import lombok.Data;
import com.stefvisser.springyield.models.Account;
import com.stefvisser.springyield.models.AccountStatus;
import com.stefvisser.springyield.models.AccountType;
import com.stefvisser.springyield.models.User;

import java.math.BigDecimal;

/**
 * Data Transfer Object for bank account profile information.
 * <p>
 * This class provides a streamlined view of bank account data for transferring between
 * service layers and client applications. It contains essential account information
 * such as the identifier, IBAN, type, and balance, while excluding sensitive or
 * unnecessary details. This lightweight representation is suitable for lists of
 * accounts in user profiles and summary displays.
 * </p>
 *
 * @since 1.0
 */
@Data
public class AccountProfileDto {
    /**
     * The unique identifier for the bank account.
     */
    private Long accountId;

    /**
     * The International Bank Account Number (IBAN) for the account.
     * <p>
     * This serves as the internationally recognized account identifier for
     * transactions and external references.
     * </p>
     */
    private String iban;

    /**
     * The user associated with this bank account.
     * <p>
     * This represents the owner of the account, providing a link to the user's
     * profile and personal information.
     * </p>
     */
    private User user;
    /**
     * The type of the bank account (e.g., CURRENT, SAVINGS).
     * <p>
     * This determines the account's features, limitations, and intended use.
     * </p>
     */
    private AccountType accountType;

    /**
     * The current balance of the account.
     * <p>
     * This represents the amount of funds available in the account at the
     * time the DTO was created.
     * </p>
     */
    private BigDecimal balance;

    /**
     * The daily limit for transactions on this account.
     * <p>
     * This indicates the maximum amount that can be transferred in a single day,
     * helping to prevent fraud and manage spending.
     * </p>
     */
    private BigDecimal dailyLimit;

    /**
     * The absolute limit for transactions on this account.
     * <p>
     * This indicates the maximum amount that can be transferred in a single transaction,
     * providing a safeguard against large unauthorized transfers.
     * </p>
     */
    private BigDecimal absoluteLimit;

    /**
     * Whether the account is active or has been deactivated.
     * <p>
     * This indicates if the account is currently active and can be used
     * for transactions, or if it has been deactivated (e.g., after user deletion).
     * </p>
     */
    private AccountStatus status;

    /**
     * Constructs an AccountProfileDto from an Account entity.
     * <p>
     * This constructor extracts the essential account information from
     * the Account entity for lightweight data transfer.
     * </p>
     *
     * @param account The Account entity to convert to a profile DTO
     */
    public AccountProfileDto(Account account) {
        this.accountId = account.getAccountId();
        this.user = account.getUser();
        this.iban = account.getIban();
        this.accountType = account.getAccountType();
        this.balance = account.getBalance();
        this.dailyLimit = account.getDailyLimit();
        this.absoluteLimit = account.getAbsoluteLimit();
        this.status = account.getStatus();
    }

    /**
     * Static factory method to create an AccountProfileDto from an Account entity.
     * <p>
     * This convenience method provides a cleaner way to convert Account entities to DTOs.
     * </p>
     *
     * @param account The Account entity to wrap
     * @return A new AccountProfileDto containing the account's profile information
     */
    public static AccountProfileDto wrap(Account account) {
        return new AccountProfileDto(account);
    }
}

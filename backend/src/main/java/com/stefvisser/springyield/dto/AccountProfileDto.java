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
 */
@Data
public class AccountProfileDto {
    /**
     * The unique identifier for the bank account.
     */
    private Long accountId;

    private String iban;
    private User user;
    private AccountType accountType;
    private BigDecimal balance;
    private BigDecimal dailyLimit;
    private BigDecimal absoluteLimit;
    private BigDecimal balanceLimit;
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
        this.balanceLimit = account.getBalanceLimit();
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

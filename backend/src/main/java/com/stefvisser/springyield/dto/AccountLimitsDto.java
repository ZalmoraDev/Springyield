package com.stefvisser.springyield.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * Data Transfer Object for account limits.
 * <p>
 * This class encapsulates the daily and absolute limits for a bank account,
 * providing a structured way to transfer limit data between service layers
 * and client applications.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountLimitsDto {
    // Getters and setters
    private BigDecimal dailyLimit;
    private BigDecimal absoluteLimit;
    private BigDecimal balanceLimit;
}

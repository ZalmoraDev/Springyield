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
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountLimitsDTO {
    // Getters and setters
    private BigDecimal dailyLimit;
    private BigDecimal absoluteLimit;

}

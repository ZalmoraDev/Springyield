package com.stefvisser.springyield.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserApprovalDto {
    private BigDecimal dailyLimit;
    private BigDecimal absoluteLimit;
    private BigDecimal balanceLimit;

    // Constructor with parameters
    public UserApprovalDto(BigDecimal dailyLimit, BigDecimal absoluteLimit) {
        this.dailyLimit = dailyLimit;
        this.absoluteLimit = absoluteLimit;
        this.balanceLimit = balanceLimit;
    }
}
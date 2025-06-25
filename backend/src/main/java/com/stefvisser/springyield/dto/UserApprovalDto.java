package com.stefvisser.springyield.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class UserApprovalDto {
    private BigDecimal dailyLimit;
    private BigDecimal absoluteLimit;
    private BigDecimal balanceLimit;

    // Constructor with parameters
    public UserApprovalDto(BigDecimal dailyLimit, BigDecimal absoluteLimit, BigDecimal balanceLimit) {
        this.dailyLimit = dailyLimit;
        this.absoluteLimit = absoluteLimit;
        this.balanceLimit = balanceLimit;
    }
}
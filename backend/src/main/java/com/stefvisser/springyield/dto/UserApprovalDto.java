package com.stefvisser.springyield.dto;

import java.math.BigDecimal;

public class UserApprovalDto {
    private BigDecimal dailyLimit;
    private BigDecimal absoluteLimit;

    // Default constructor
    public UserApprovalDto() {
    }

    // Constructor with parameters
    public UserApprovalDto(BigDecimal dailyLimit, BigDecimal absoluteLimit) {
        this.dailyLimit = dailyLimit;
        this.absoluteLimit = absoluteLimit;
    }

    // Getters and setters
    public BigDecimal getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(BigDecimal dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public BigDecimal getAbsoluteLimit() {
        return absoluteLimit;
    }

    public void setAbsoluteLimit(BigDecimal absoluteLimit) {
        this.absoluteLimit = absoluteLimit;
    }

    // toString method for debugging
    @Override
    public String toString() {
        return "UserApprovalDTO{" +
                "dailyLimit=" + dailyLimit +
                ", absoluteLimit=" + absoluteLimit +
                '}';
    }
}
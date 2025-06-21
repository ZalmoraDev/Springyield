package com.stefvisser.springyield.config;

import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class BankConfiguration {
    public static final String BANK_IBAN = "NL69SPYD694206942069";
    public static final String BANK_NAME = "SPRINGYIELD Bank";

    public static final BigDecimal BANK_INITIAL_BALANCE = new BigDecimal("1000000000.00"); // 1 billion
}

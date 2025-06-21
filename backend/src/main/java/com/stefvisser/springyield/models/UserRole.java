package com.stefvisser.springyield.models;

import org.springframework.security.core.GrantedAuthority;

public enum UserRole implements GrantedAuthority {
    UNAPPROVED,
    APPROVED,
    EMPLOYEE,
    ADMIN;

    public String getAuthority() {
        return name();
    }
}

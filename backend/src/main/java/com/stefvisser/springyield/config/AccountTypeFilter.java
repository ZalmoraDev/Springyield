package com.stefvisser.springyield.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.stefvisser.springyield.models.User;
import com.stefvisser.springyield.models.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AccountTypeFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();

            if (principal instanceof UserDetails userDetails) {
                // Assuming you have a way to fetch the User object from UserDetails
                User user = (User) userDetails;

                if (user.getRole().equals(UserRole.UNAPPROVED)) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Account is not approved.");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
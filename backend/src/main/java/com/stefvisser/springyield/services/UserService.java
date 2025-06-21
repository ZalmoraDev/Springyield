package com.stefvisser.springyield.services;

import com.stefvisser.springyield.dto.PaginatedDataDTO;
import com.stefvisser.springyield.dto.UserSignupDto;
import com.stefvisser.springyield.dto.UserLoginDto;
import com.stefvisser.springyield.dto.UserProfileDto;
import com.stefvisser.springyield.dto.UserUpdateDto;
import com.stefvisser.springyield.models.User;
import com.stefvisser.springyield.models.UserRole;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.util.List;

/// Because @Service is used in the implementation
public interface UserService {

    UserProfileDto login(UserLoginDto loginDto);

    User signupUser(UserSignupDto userSignupDto);

    List<User> getAllUsers();

    User getUserById(Long userId);

    User getUserByEmail(String email);

    PaginatedDataDTO<UserProfileDto> search(String query, UserRole role, int limit, int offset, boolean isAdmin);

    void addRandomUsers(int count);
    void addDefaultUsers();
    UserProfileDto approveUser(Long userId, BigDecimal dailyLimit, BigDecimal absoluteLimit);

    void deleteUser(Long userId);

    UserProfileDto updateUser(Long userId, UserUpdateDto userUpdateDto);

    BCryptPasswordEncoder getPasswordEncoder();
}

package com.stefvisser.springyield.services;

import com.stefvisser.springyield.dto.PaginatedDataDto;
import com.stefvisser.springyield.dto.UserProfileDto;
import com.stefvisser.springyield.dto.UserUpdateDto;
import com.stefvisser.springyield.models.User;
import com.stefvisser.springyield.models.UserRole;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.util.List;

/// Because @Service is used in the implementation
public interface UserService {
    List<User> getAllUsers();

    User getUserById(Long userId);

    PaginatedDataDto<UserProfileDto> search(String query, UserRole role, int limit, int offset, boolean isAdmin);

    void addRandomUsers(int count);
    void addDefaultUsers();
    UserProfileDto approveUser(Long userId, BigDecimal dailyLimit, BigDecimal absoluteLimit);

    void deleteUser(User execUser, Long targetUserId);

    UserProfileDto updateUser(Long userId, UserUpdateDto userUpdateDto);

    BCryptPasswordEncoder getPasswordEncoder();

    User findByEmail(String mail);
}

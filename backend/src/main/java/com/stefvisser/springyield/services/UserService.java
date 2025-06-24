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


    PaginatedDataDto<UserProfileDto> search(String query, UserRole role, int limit, int offset, boolean isAdmin);

    void approveUser(User execUser, Long userId, BigDecimal dailyLimit, BigDecimal absoluteLimit);

    User getUserById(User execUser, Long targetUserId);
    void deleteUser(User execUser, Long targetUserId);
    UserProfileDto updateUser(User execUser, Long targetUserId, UserUpdateDto userUpdateDto);

    User findByEmail(String mail);
}

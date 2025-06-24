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
    // API methods
    User getUserById(User execUser, Long targetUserId);
    PaginatedDataDto<UserProfileDto> search(User execUser, String query, UserRole role, int limit, int offset);

    void approveUser(User execUser, Long userId, BigDecimal dailyLimit, BigDecimal absoluteLimit);
    UserProfileDto updateUser(User execUser, Long targetUserId, UserUpdateDto userUpdateDto);
    void deleteUser(User execUser, Long targetUserId);


    // Non-API methods
    User findByEmail(String mail);
    void saveAll(List<User> users);
    void save(User user);
}

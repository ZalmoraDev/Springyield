package com.stefvisser.springyield.repositories;

import jakarta.validation.constraints.Email;
import com.stefvisser.springyield.dto.PaginatedDataDto;
import com.stefvisser.springyield.dto.UserProfileDto;
import com.stefvisser.springyield.models.User;
import com.stefvisser.springyield.models.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {


    /// Return status on whether a user with the given email exists in the database
    /// when creating a user
    boolean existsByEmail(String email);

    User findByUserId(Long userId);

    User findByEmail(String email);

    default List<User> findByName(String firstName, String lastName) {
        return this.findAll().stream().filter(user ->
                user.getFirstName().toLowerCase(Locale.ROOT).contains(firstName.toLowerCase(Locale.ROOT))
                        && user.getLastName().toLowerCase(Locale.ROOT).contains(lastName.toLowerCase(Locale.ROOT))
        ).toList();
    }

    default PaginatedDataDto<UserProfileDto> search(String query, UserRole role, int limit, int offset, boolean isAdmin) {
        if (limit <= 0 || offset < 0)
            throw new IllegalArgumentException("Limit must be greater than 0 and offset must be non-negative.");
        if (query == null) query = "";

        String queryLower = query.toLowerCase(Locale.ROOT);
        List<User> allUsers = this.findAll();

        Stream<User> userStream = allUsers.stream();

        if (role != null) {
            userStream = userStream.filter(user -> user.getRole().equals(role));
        }

        if (!query.isBlank()) {
            userStream = userStream.filter(user ->
                    user.getFirstName().toLowerCase(Locale.ROOT).contains(queryLower)
                    || user.getLastName().toLowerCase(Locale.ROOT).contains(queryLower)
                    || user.getEmail().toLowerCase(Locale.ROOT).contains(queryLower)
                    || String.valueOf(user.getBsnNumber()).contains(queryLower)
            );
        }

        // If the user is not an admin, filter out ADMIN and EMPLOYEE roles
        if (!isAdmin) {
            userStream = userStream.filter(user -> !user.getRole().equals(UserRole.ADMIN)
                    && !user.getRole().equals(UserRole.EMPLOYEE));
        }

        List<User> filteredUsers = userStream.toList(); // Collect the filtered users into a list

        int totalCount = filteredUsers.size(); // Get the total count from the list size

        List<UserProfileDto> paginatedUsers = filteredUsers.stream() // Create a new stream from the filtered list for pagination
                .sorted(Comparator.comparing(User::getUserId).reversed())
                .skip(offset)
                .limit(limit)
                .map(UserProfileDto::wrap)
                .toList();

        return new PaginatedDataDto<>(paginatedUsers, totalCount);
    }
}

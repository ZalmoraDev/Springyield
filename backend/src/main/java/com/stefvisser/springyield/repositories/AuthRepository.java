package com.stefvisser.springyield.repositories;

import com.stefvisser.springyield.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthRepository extends JpaRepository<User, Integer> {
    /// Return status on whether a user with the given email exists in the database
    /// when creating a user
    boolean existsByEmail(String email);
    User findByEmail(String email);
}

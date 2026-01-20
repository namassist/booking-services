package com.example.booking_service.repository;

import com.example.booking_service.entity.User;
import com.example.booking_service.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find a user by email.
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user exists with the given email.
     */
    boolean existsByEmail(String email);

    /**
     * Find all users with a specific role.
     */
    List<User> findByRole(UserRole role);

    /**
     * Find all active users.
     */
    List<User> findByIsActiveTrue();
}

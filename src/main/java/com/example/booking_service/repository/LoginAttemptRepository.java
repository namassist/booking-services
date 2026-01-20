package com.example.booking_service.repository;

import com.example.booking_service.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Repository for LoginAttempt entity.
 */
@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, UUID> {

    /**
     * Count failed login attempts for a user since a given time.
     */
    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.user.id = :userId " +
           "AND la.success = false AND la.attemptTime >= :since")
    long countFailedAttemptsSince(
            @Param("userId") UUID userId,
            @Param("since") OffsetDateTime since
    );

    /**
     * Delete old login attempts (cleanup).
     */
    @Query("DELETE FROM LoginAttempt la WHERE la.attemptTime < :before")
    void deleteOldAttempts(@Param("before") OffsetDateTime before);
}

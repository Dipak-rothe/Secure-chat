package com.dipak.server.repository;

import com.dipak.server.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Search users by name (case-insensitive prefix) or email, excluding the
     * requesting user and any users they have blocked.
     * Blocked-user exclusion is handled in the service layer for simplicity.
     */
    @Query("SELECT u FROM User u WHERE (LOWER(u.name) LIKE LOWER(CONCAT(:q, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT(:q, '%'))) AND u.id <> :excludeId")
    List<User> searchByNameOrEmail(@Param("q") String query, @Param("excludeId") Long excludeId);
}

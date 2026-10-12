package com.dipak.server.repository;

import com.dipak.server.domain.UserKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserKeyRepository extends JpaRepository<UserKey, Long> {
    Optional<UserKey> findByUserId(Long userId);
}

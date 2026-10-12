package com.dipak.server.service;

import com.dipak.server.domain.User;
import com.dipak.server.domain.UserKey;
import com.dipak.server.dto.UserDto;
import com.dipak.server.dto.UserKeyDto;
import com.dipak.server.exception.AppException;
import com.dipak.server.repository.UserKeyRepository;
import com.dipak.server.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepo;
    private final UserKeyRepository keyRepo;

    public UserService(UserRepository userRepo, UserKeyRepository keyRepo) {
        this.userRepo = userRepo;
        this.keyRepo = keyRepo;
    }

    public UserDto getMe(Long userId) {
        User user = getOrThrow(userId);
        return new UserDto(user);
    }

    /**
     * Search users by name or email prefix. Excludes the requesting user.
     * Minimum 2, maximum 10 characters to prevent full-scan abuse (further protected
     * by rate limiting in the controller).
     */
    public List<UserDto> search(String query, Long currentUserId) {
        String q = query.trim();
        if (q.length() < 2 || q.length() > 10) {
            throw AppException.badRequest("Search query must be 2-10 characters");
        }
        return userRepo.searchByNameOrEmail(q, currentUserId)
                .stream()
                .map(UserDto::new)
                .collect(Collectors.toList());
    }

    /** Upload or update a user's public key bundle. */
    @Transactional
    public UserKeyDto uploadKeys(Long userId, UserKeyDto dto) {
        User user = getOrThrow(userId);
        UserKey key = keyRepo.findByUserId(userId).orElse(new UserKey());
        key.setUser(user);
        key.setPublicKey(dto.getPublicKey());
        key.setKeyFingerprint(dto.getKeyFingerprint());
        key.setEncryptedPrivateKeyBackup(dto.getEncryptedPrivateKeyBackup());
        key.setKdfSalt(dto.getKdfSalt());
        key.setKdfIterations(dto.getKdfIterations());
        return new UserKeyDto(keyRepo.save(key));
    }

    /** Fetch another user's public key (needed to derive a shared secret for E2E). */
    public UserKeyDto getKeys(Long targetUserId) {
        return keyRepo.findByUserId(targetUserId)
                .map(UserKeyDto::new)
                .orElseThrow(() -> AppException.notFound("No keys found for user " + targetUserId));
    }

    private User getOrThrow(Long userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));
    }
}

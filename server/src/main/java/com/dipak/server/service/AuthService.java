package com.dipak.server.service;

import com.dipak.server.domain.User;
import com.dipak.server.dto.*;
import com.dipak.server.exception.AppException;
import com.dipak.server.repository.UserRepository;
import com.dipak.server.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles registration, login, token refresh, and logout.
 * BCrypt cost-12 password hashing. Never logs passwords or tokens.
 */
@Service
public class AuthService {

    private final UserRepository userRepo;
    private final JwtService jwtService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepo,
                       JwtService jwtService,
                       TokenService tokenService,
                       PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.jwtService = jwtService;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }

    /** Register a new user. Returns auth tokens on success. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        if (userRepo.existsByEmail(email)) {
            throw AppException.conflict("Email already registered");
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        // Never store the raw password — only the BCrypt hash
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user = userRepo.save(user);

        return buildAuthResponse(user);
    }

    /** Authenticate a user. Returns auth tokens on success. */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> AppException.unauthorized("Invalid email or password"));

        // Timing-safe comparison via BCrypt
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw AppException.unauthorized("Invalid email or password");
        }

        return buildAuthResponse(user);
    }

    /** Rotate a refresh token. Returns new access + refresh tokens. */
    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        User user = tokenService.rotateRefreshToken(request.getRefreshToken());
        return buildAuthResponse(user);
    }

    /** Revoke all refresh tokens for the user (logout). */
    @Transactional
    public void logout(Long userId) {
        tokenService.revokeAll(userId);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = tokenService.createRefreshToken(user);
        return new AuthResponse(accessToken, refreshToken, new UserDto(user));
    }
}

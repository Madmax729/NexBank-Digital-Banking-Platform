package com.banking.auth.service;

import com.banking.auth.dto.*;
import com.banking.auth.entity.RefreshToken;
import com.banking.auth.entity.User;
import com.banking.auth.exception.AuthException;
import com.banking.auth.exception.TokenRefreshException;
import com.banking.auth.repository.RefreshTokenRepository;
import com.banking.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .roles(Set.of(User.Role.USER))
                .status(User.UserStatus.ACTIVE)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getId());

        // Publish audit event
        kafkaTemplate.send("audit-events",
                String.format("{\"action\":\"USER_REGISTERED\",\"userId\":\"%s\",\"email\":\"%s\",\"timestamp\":\"%s\"}",
                        user.getId(), user.getEmail(), LocalDateTime.now()));

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("User not found"));

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new AuthException("Account is " + user.getStatus().name().toLowerCase());
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Publish audit event
        kafkaTemplate.send("audit-events",
                String.format("{\"action\":\"USER_LOGIN\",\"userId\":\"%s\",\"email\":\"%s\",\"timestamp\":\"%s\"}",
                        user.getId(), user.getEmail(), LocalDateTime.now()));

        log.info("User logged in successfully: {}", user.getId());
        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(TokenRefreshRequest request) {
        String requestToken = request.getRefreshToken();

        RefreshToken refreshToken = refreshTokenRepository.findByToken(requestToken)
                .orElseThrow(() -> new TokenRefreshException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new TokenRefreshException("Refresh token has been revoked");
        }

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new TokenRefreshException("Refresh token has expired. Please login again");
        }

        // Revoke old token and create new one
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        User user = refreshToken.getUser();
        return generateAuthResponse(user);
    }

    @Transactional
    public void logout(String accessToken, String userId) {
        log.info("Logging out user: {}", userId);

        // Blacklist the access token in Redis
        if (accessToken != null && !accessToken.isEmpty()) {
            redisTemplate.opsForValue().set(
                    "blacklist:" + accessToken, "true",
                    jwtService.getExpirationMs(), TimeUnit.MILLISECONDS
            );
        }

        // Revoke all refresh tokens for the user
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new AuthException("User not found"));
        refreshTokenRepository.revokeAllByUser(user);

        // Publish audit event
        kafkaTemplate.send("audit-events",
                String.format("{\"action\":\"USER_LOGOUT\",\"userId\":\"%s\",\"timestamp\":\"%s\"}",
                        userId, LocalDateTime.now()));

        log.info("User logged out successfully: {}", userId);
    }

    private AuthResponse generateAuthResponse(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        String accessToken = jwtService.generateAccessToken(
                user.getId().toString(), user.getEmail(), roles);

        RefreshToken refreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .userId(user.getId().toString())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(roles)
                .expiresIn(jwtService.getExpirationMs() / 1000)
                .build();
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }
}

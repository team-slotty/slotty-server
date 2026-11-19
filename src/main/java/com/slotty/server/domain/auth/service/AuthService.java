package com.slotty.server.domain.auth.service;

import com.slotty.server.domain.auth.dto.LoginRequest;
import com.slotty.server.domain.auth.dto.TokenResponse;
import com.slotty.server.domain.user.User;
import com.slotty.server.domain.user.UserRepository;
import com.slotty.server.global.auth.JwtProvider;
import com.slotty.server.global.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

import static com.slotty.server.global.exception.ErrorCode.INVALID_LOGIN;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final String REFRESH_KEY_PREFIX = "refresh:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;
    private final Duration refreshTtl;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtProvider jwtProvider,
                       StringRedisTemplate redisTemplate,
                       @Value("${jwt.refresh-expiration}") long refreshExpiration) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.redisTemplate = redisTemplate;
        this.refreshTtl = Duration.ofMillis(refreshExpiration);
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByLoginIdAndDeletedAtIsNull(request.loginId())
                .orElseThrow(() -> new BusinessException(INVALID_LOGIN));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(INVALID_LOGIN);
        }

        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = UUID.randomUUID().toString();

        redisTemplate.opsForValue().set(
                REFRESH_KEY_PREFIX + refreshToken,
                String.valueOf(user.getId()),
                refreshTtl
        );

        return new TokenResponse(accessToken, refreshToken);
    }
}
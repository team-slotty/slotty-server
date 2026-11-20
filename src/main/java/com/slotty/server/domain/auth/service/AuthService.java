package com.slotty.server.domain.auth.service;

import com.slotty.server.domain.auth.dto.LoginRequest;
import com.slotty.server.domain.auth.dto.LogoutRequest;
import com.slotty.server.domain.auth.dto.ReissueRequest;
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
import static com.slotty.server.global.exception.ErrorCode.INVALID_REFRESH_TOKEN;

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

        return issueTokens(user.getId());
    }

    public TokenResponse reissue(ReissueRequest reissueRequest) {

        // 값을 읽는 것과 지우는 것을 한 번에 수행
        // 키가 있으면 userId 문자열을 돌려주고 즉시 지우고, 키가 없거나 TTL로 만료된 경우 null 반환
        // 이 줄이 실행되는 순간 옛 Refresh는 더 이상 사용할 수 없다
        String userIdValue = redisTemplate.opsForValue()
                .getAndDelete(REFRESH_KEY_PREFIX + reissueRequest.refreshToken());

        if (userIdValue == null) {
            throw new BusinessException(INVALID_REFRESH_TOKEN);
        }

        Long userId = Long.valueOf(userIdValue);
        if(!userRepository.existsByIdAndDeletedAtIsNull(userId)) { // 탈퇴한 사용자면 거부
            throw new BusinessException(INVALID_REFRESH_TOKEN);
        }

        return issueTokens(userId);
    }

    // Refresh를 Redis에서 삭제해 이 기기의 재발급을 막는다
    // 이미 없는 키여도 결과(로그아웃 상태)는 같으므로 에러 없이 끝낸다
    public void logout(LogoutRequest logoutRequest) {
        redisTemplate.delete(REFRESH_KEY_PREFIX + logoutRequest.refreshToken());
    }

    // 새 Access와 새 Refresh를 만들고, 새 Refresh를 Redis에 저장
    private TokenResponse issueTokens(Long userId) {

        String accessToken = jwtProvider.createAccessToken(userId);
        String refreshToken = UUID.randomUUID().toString();

        redisTemplate.opsForValue().set(
                REFRESH_KEY_PREFIX + refreshToken,
                String.valueOf(userId),
                refreshTtl
        );

        return new TokenResponse(accessToken, refreshToken);
    }
}
package com.slotty.server.domain.auth.service;

import com.slotty.server.domain.auth.dto.LoginRequest;
import com.slotty.server.domain.auth.dto.LogoutRequest;
import com.slotty.server.domain.auth.dto.ReissueRequest;
import com.slotty.server.domain.auth.dto.TokenResponse;
import com.slotty.server.domain.user.User;
import com.slotty.server.domain.user.UserRepository;
import com.slotty.server.global.auth.JwtProvider;
import com.slotty.server.global.exception.BusinessException;
import com.slotty.server.domain.auth.dto.SignupRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Duration;
import java.util.UUID;

import static com.slotty.server.global.exception.ErrorCode.INVALID_LOGIN;
import static com.slotty.server.global.exception.ErrorCode.INVALID_REFRESH_TOKEN;
import static com.slotty.server.global.exception.ErrorCode.LOGIN_ID_DUPLICATED;

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

    // 회원가입 → 바로 로그인 상태 (가입 다음 화면이 빌라 만들기/초대 코드라 토큰이 필요)
    @Transactional
    public TokenResponse signup(SignupRequest request) {
        if (userRepository.existsByLoginId(request.loginId())) {
            throw new BusinessException(LOGIN_ID_DUPLICATED);
        }

        User user = User.create(
                request.loginId(),
                passwordEncoder.encode(request.password()), // 비밀번호는 BCrypt 해시로만 저장
                request.name().strip(),
                request.phone().replace("-", ""),           // 하이픈 제거, 숫자만
                request.phonePublic()
        );

        try {
            userRepository.saveAndFlush(user); // 동시에 같은 아이디로 가입하면 uk_users_login_id 위반
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(LOGIN_ID_DUPLICATED);
        }

        return issueTokens(user.getId());
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

    // 탈퇴: 이 사용자의 Refresh를 모든 기기에서 폐기
    // 저장 구조가 refresh:{토큰} → userId 라서 사용자별로 바로 찾을 수 없음 → SCAN으로 refresh:* 키를 훑어 값이 userId인 것만 삭제
    // (KEYS는 Redis를 멈추게 할 수 있어 SCAN으로 나눠 읽음. 재발급도 탈퇴 사용자를 거부하므로 이건 이중 안전장치)
    public void revokeAllRefreshTokens(Long userId) {
        String target = String.valueOf(userId);
        ScanOptions options = ScanOptions.scanOptions().match(REFRESH_KEY_PREFIX + "*").count(500).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                if (target.equals(redisTemplate.opsForValue().get(key))) {
                    redisTemplate.delete(key);
                }
            }
        }
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

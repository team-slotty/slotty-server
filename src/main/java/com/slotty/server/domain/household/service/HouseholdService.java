package com.slotty.server.domain.household.service;

import com.slotty.server.domain.household.dto.JoinRequest;
import com.slotty.server.domain.household.dto.JoinResponse;
import com.slotty.server.domain.household.dto.MyHouseholdResponse;
import com.slotty.server.domain.household.dto.MyHouseholdResponse.AssignedSlot;
import com.slotty.server.domain.household.dto.MyHouseholdResponse.Member;
import com.slotty.server.domain.slot.ParkingSlotRepository;
import com.slotty.server.domain.user.User;
import com.slotty.server.domain.user.UserRepository;
import com.slotty.server.domain.villa.Household;
import com.slotty.server.domain.villa.HouseholdRepository;
import com.slotty.server.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

import static com.slotty.server.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HouseholdService {

    private static final String JOIN_ATTEMPT_KEY_PREFIX = "join-attempt:";
    private static final int MAX_JOIN_FAILURES = 5;                   // 틀린 코드 5번까지
    private static final Duration JOIN_LOCK_TIME = Duration.ofMinutes(10); // 첫 실패부터 10분 동안 셈

    private final UserRepository userRepository;
    private final HouseholdRepository householdRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final StringRedisTemplate redisTemplate;

    // ===== 19. 초대 코드로 세대 참여 =====
    @Transactional
    public JoinResponse join(Long userId, JoinRequest request) {
        User user = userRepository.findActiveWithOptionalHousehold(userId)
                .orElseThrow(() -> new BusinessException(UNAUTHORIZED));
        if (user.getHousehold() != null) {
            throw new BusinessException(ALREADY_IN_HOUSEHOLD);
        }

        String attemptKey = JOIN_ATTEMPT_KEY_PREFIX + userId;
        String failures = redisTemplate.opsForValue().get(attemptKey);
        if (failures != null && Integer.parseInt(failures) >= MAX_JOIN_FAILURES) {
            throw new BusinessException(TOO_MANY_ATTEMPTS);
        }

        String inviteCode = request.inviteCode().strip().toUpperCase(); // 공백·소문자 입력도 허용
        Household household = householdRepository.findWithVillaByInviteCode(inviteCode)
                .orElseThrow(() -> {
                    recordFailure(attemptKey);
                    return new BusinessException(INVITE_CODE_INVALID);
                });

        user.joinHousehold(household); // dirty checking으로 users.household_id UPDATE
        redisTemplate.delete(attemptKey); // 성공하면 실패 횟수 초기화

        boolean hasAssignedSlot = parkingSlotRepository.existsByHouseholdId(household.getId());
        return JoinResponse.of(household, hasAssignedSlot);
    }

    // ===== 20. 내 세대 =====
    public MyHouseholdResponse getMyHousehold(Long userId) {
        User user = userRepository.findActiveWithHouseholdAndVilla(userId) // 소속 전이면 403
                .orElseThrow(() -> new BusinessException(NOT_IN_HOUSEHOLD));
        Household household = user.getHousehold();

        List<AssignedSlot> assignedSlots = parkingSlotRepository.findByHouseholdIdOrderById(household.getId())
                .stream()
                .map(slot -> new AssignedSlot(slot.getId(), slot.getLabel()))
                .toList();
        List<Member> members = userRepository.findByHouseholdIdAndDeletedAtIsNullOrderById(household.getId())
                .stream()
                .map(member -> new Member(member.getId(), member.getName()))
                .toList();

        return new MyHouseholdResponse(household.getId(), household.getUnitNumber(), assignedSlots, members);
    }

    // 실패 횟수 +1. 첫 실패일 때만 만료 시간을 걸어 "첫 실패부터 10분" 동안 셈
    private void recordFailure(String attemptKey) {
        Long count = redisTemplate.opsForValue().increment(attemptKey);
        if (count != null && count == 1) {
            redisTemplate.expire(attemptKey, JOIN_LOCK_TIME);
        }
    }
}

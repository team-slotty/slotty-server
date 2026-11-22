package com.slotty.server.domain.vehicle.service;

import com.slotty.server.domain.user.User;
import com.slotty.server.domain.user.UserRepository;
import com.slotty.server.domain.vehicle.Vehicle;
import com.slotty.server.domain.vehicle.VehicleRepository;
import com.slotty.server.domain.vehicle.dto.VehicleSearchResponse;
import com.slotty.server.domain.vehicle.dto.VehicleSearchResponse.Contact;
import com.slotty.server.domain.vehicle.dto.VehicleSearchResponse.Result;
import com.slotty.server.domain.vehicle.dto.VehicleSearchResponse.VisitInfo;
import com.slotty.server.domain.slot.ParkingSlot;
import com.slotty.server.domain.villa.Household;
import com.slotty.server.domain.visit.Visit;
import com.slotty.server.domain.visit.VisitRepository;
import com.slotty.server.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static com.slotty.server.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleSearchService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String SEARCH_COUNT_KEY_PREFIX = "search-count:";
    private static final int DAILY_LIMIT = 20;
    private static final int MAX_RESULTS = 10;
    private static final long VISIT_LOOKAHEAD_HOURS = 24; // 24시간 안에 시작하는 방문까지 검색

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final VisitRepository visitRepository;
    private final StringRedisTemplate redisTemplate;

    // ===== 29. 차량번호 조회 =====
    public VehicleSearchResponse search(Long userId, String plate) {
        User user = userRepository.findActiveWithHouseholdAndVilla(userId)
                .orElseThrow(() -> new BusinessException(NOT_IN_HOUSEHOLD));
        Household myHousehold = user.getHousehold();

        // 1. 입력 검사 (잘못된 입력은 횟수에 세지 않음)
        String keyword = plate.replaceAll("\\s", "");
        if (keyword.length() < 4) {
            throw new BusinessException(PLATE_TOO_SHORT);
        }
        String last4 = keyword.substring(keyword.length() - 4);
        if (!last4.chars().allMatch(Character::isDigit)) { // 차량번호는 항상 숫자 4자리로 끝남
            throw new BusinessException(VALIDATION_FAILED);
        }

        // 2. 하루 20회 제한 (결과가 없어도 1회)
        int used = increaseTodayCount(userId);
        if (used > DAILY_LIMIT) {
            throw new BusinessException(SEARCH_LIMIT_EXCEEDED);
        }

        // 3. 우리 빌라 차량 중 뒤 4자리가 같고(인덱스) 입력값으로 끝나는 차, 최대 10건
        Long villaId = myHousehold.getVilla().getId();
        List<Result> results = new ArrayList<>(vehicleRepository
                .searchInVilla(villaId, last4, keyword, PageRequest.of(0, MAX_RESULTS))
                .stream()
                .map(vehicle -> toResult(vehicle, myHousehold))
                .toList());

        // 4. 남은 자리에 방문차량: 종료 안 됐고 진행 중(확인 필요 포함)이거나 24시간 안에 시작하는 방문
        int remaining = MAX_RESULTS - results.size();
        if (remaining > 0) {
            LocalDateTime now = LocalDateTime.now(KST).truncatedTo(ChronoUnit.SECONDS);
            boolean admin = isAdmin(user);
            visitRepository.searchInVilla(villaId, last4, keyword, now, now.plusHours(VISIT_LOOKAHEAD_HOURS),
                            PageRequest.of(0, remaining))
                    .forEach(visit -> results.add(toVisitResult(visit, myHousehold, admin)));
        }

        return new VehicleSearchResponse(DAILY_LIMIT - used, results);
    }

    private Result toResult(Vehicle vehicle, Household myHousehold) {
        boolean myVehicle = vehicle.getHousehold().getId().equals(myHousehold.getId());
        User contactUser = vehicle.getContactUser();
        boolean canCall = contactUser != null && contactUser.isPhonePublic();
        return new Result(
                "RESIDENT",
                vehicle.getHousehold().getUnitNumber(),
                vehicle.getPlateLast4(),
                myVehicle ? vehicle.getPlateNumber() : null,
                null,
                new Contact(canCall, canCall ? contactUser.getPhone() : null)
        );
    }

    // 방문차량 결과: 호수와 구분(VISIT)은 누구나, 방문 시간·면은 관리자·면 주인·방문 세대만 (F8)
    private Result toVisitResult(Visit visit, Household myHousehold, boolean admin) {
        boolean myVisit = visit.getHousehold().getId().equals(myHousehold.getId());
        ParkingSlot slot = visit.hasActiveSlot() ? visit.getSlot() : null;
        boolean slotOwner = slot != null && slot.getHousehold() != null
                && slot.getHousehold().getId().equals(myHousehold.getId());
        User registrant = visit.getRegisteredBy();
        boolean canCall = registrant.isPhonePublic() && registrant.getDeletedAt() == null;
        return new Result(
                "VISIT",
                visit.getHousehold().getUnitNumber(),
                visit.getPlateLast4(),
                myVisit ? visit.getPlateNumber() : null,
                (admin || slotOwner || myVisit)
                        ? new VisitInfo(visit.getId(), visit.getStartAt(), visit.getEndAt(), slot == null ? null : slot.getLabel())
                        : null,
                new Contact(canCall, canCall ? registrant.getPhone() : null)
        );
    }

    private boolean isAdmin(User user) {
        User admin = user.getHousehold().getVilla().getAdminUser();
        return admin != null && admin.getId().equals(user.getId());
    }

    // 오늘(KST) 검색 횟수 +1 후 현재 횟수 반환. 첫 검색일 때 자정까지 남은 시간으로 만료
    private int increaseTodayCount(Long userId) {
        LocalDate today = LocalDate.now(KST);
        String key = SEARCH_COUNT_KEY_PREFIX + userId + ":" + today; // 예: search-count:1:2026-11-22
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            Duration untilMidnight = Duration.between(LocalDateTime.now(KST), today.plusDays(1).atStartOfDay());
            redisTemplate.expire(key, untilMidnight);
        }
        return count == null ? 0 : count.intValue();
    }
}

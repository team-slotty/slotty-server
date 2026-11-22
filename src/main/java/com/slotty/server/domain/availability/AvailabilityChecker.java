package com.slotty.server.domain.availability;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

// "이 면이 지금 가용시간 회차인가?" (면 상세·현황판에서 같은 규칙을 쓰도록 한 곳에)
@Component
@RequiredArgsConstructor
public class AvailabilityChecker {

    private final AvailabilityRepository availabilityRepository;
    private final AvailabilityExceptionRepository availabilityExceptionRepository;

    // 지금 열려 있고, 오늘 [이번만 해제]·주인 입차로 해제되지 않은 가용시간 (없으면 empty)
    public Optional<Availability> findOpenNow(Long slotId, LocalDateTime now) {
        return availabilityRepository.findBySlotId(slotId).stream()
                .filter(availability -> availability.isOpenAt(now))
                .filter(availability -> !availabilityExceptionRepository
                        .existsByAvailabilityIdAndSkipDate(availability.getId(), now.toLocalDate()))
                .findFirst();
    }

    // 현황판용: 빌라의 면별로 지금 열린 가용시간 (면 id → 가용시간). 가용시간 1번 + 해제 예외 1번, 쿼리 2번
    public Map<Long, Availability> findOpenNowByVillaId(Long villaId, LocalDateTime now) {
        List<Availability> availabilities = availabilityRepository.findBySlotVillaId(villaId);
        if (availabilities.isEmpty()) {
            return Map.of();
        }
        LocalDate today = now.toLocalDate();
        Set<Long> skippedToday = availabilityExceptionRepository
                .findByAvailabilityIdInAndSkipDateGreaterThanEqualOrderBySkipDate(
                        availabilities.stream().map(Availability::getId).toList(), today)
                .stream()
                .filter(e -> e.getSkipDate().equals(today))
                .map(e -> e.getAvailability().getId())
                .collect(Collectors.toSet());

        return availabilities.stream()
                .filter(a -> a.isOpenAt(now))
                .filter(a -> !skippedToday.contains(a.getId()))
                .collect(Collectors.toMap(a -> a.getSlot().getId(), a -> a, (first, second) -> first)); // 겹치면 먼저 것
    }
}

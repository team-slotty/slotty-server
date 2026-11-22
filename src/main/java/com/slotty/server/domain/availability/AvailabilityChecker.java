package com.slotty.server.domain.availability;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

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
}

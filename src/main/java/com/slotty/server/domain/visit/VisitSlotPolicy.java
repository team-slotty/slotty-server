package com.slotty.server.domain.visit;

import com.slotty.server.domain.availability.Availability;
import com.slotty.server.domain.availability.AvailabilityExceptionRepository;
import com.slotty.server.domain.availability.AvailabilityRepository;
import com.slotty.server.domain.slot.ParkingSlot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

// "이 면을 이 방문 시간에 지정할 수 있는가?" (등록 35, 지정 가능 면 36, 수정 39, 가용시간 변경 21·49·50이 같은 규칙을 쓰도록 한 곳에)
// 공용 면은 언제나 가능. 배정 면은 방문 시간 전체가 그 면의 가용시간(해제 예외 제외) 안에 들어가야 함 (내 면도 같음, 명세 확정 필요 11)
@Component
@RequiredArgsConstructor
public class VisitSlotPolicy {

    private static final int DAY_SECONDS = 24 * 60 * 60;

    private final AvailabilityRepository availabilityRepository;
    private final AvailabilityExceptionRepository availabilityExceptionRepository;
    private final VisitRepository visitRepository;

    // 가용시간이 구간 [start, end)를 빈틈없이 덮는가
    public boolean covers(ParkingSlot slot, LocalDateTime start, LocalDateTime end) {
        return findWindow(slot, start, end).isPresent();
    }

    // 덮으면 시작하는 날의 가용 구간을 돌려줌 (36번 응답의 allDay·window). 공용 면은 Window.COMMON
    public Optional<Window> findWindow(ParkingSlot slot, LocalDateTime start, LocalDateTime end) {
        if (slot.getHousehold() == null) {
            return Optional.of(Window.COMMON);
        }
        List<Availability> availabilities = availabilityRepository.findBySlotId(slot.getId());
        if (availabilities.isEmpty() || !start.isBefore(end)) {
            return Optional.empty();
        }
        Set<String> skipped = loadSkipped(availabilities, start.toLocalDate());

        // 방문은 최대 24시간이라 이틀에 걸칠 수 있음 → 날짜별로 잘라서 각 날을 검사
        Window first = null;
        for (LocalDate day = start.toLocalDate(); day.atStartOfDay().isBefore(end); day = day.plusDays(1)) {
            LocalDateTime dayStart = day.atStartOfDay();
            int from = start.isAfter(dayStart) ? start.toLocalTime().toSecondOfDay() : 0;
            int to = end.isBefore(dayStart.plusDays(1)) ? end.toLocalTime().toSecondOfDay() : DAY_SECONDS;

            Optional<int[]> block = coveringBlock(mergedBlocks(availabilities, skipped, day), from, to);
            if (block.isEmpty()) {
                return Optional.empty();
            }
            if (first == null) {
                first = Window.of(block.get());
            }
        }
        return Optional.ofNullable(first);
    }

    // 가용시간 삭제·[이번만 해제]·면 배정 변경 뒤 호출: 근거가 사라진 미종료 방문에 slotCancelledAt 기록 (명세 확정 필요 10)
    // 이미 지난 부분은 따지지 않고 지금부터 남은 시간만 검사
    public int cancelBrokenVisits(ParkingSlot slot, LocalDateTime now) {
        int cancelled = 0;
        for (Visit visit : visitRepository.findActiveBySlotId(slot.getId(), now)) {
            LocalDateTime from = visit.getStartAt().isAfter(now) ? visit.getStartAt() : now;
            if (!covers(slot, from, visit.getEndAt())) {
                visit.cancelSlot(now);
                cancelled++;
            }
        }
        return cancelled;
    }

    // 그 날 회차가 있고 해제되지 않은 가용시간들을 [시작초, 끝초] 구간으로 바꿔 이어지는 것끼리 합침
    // 예: 09:00~12:00 + 12:00~18:00 → 09:00~18:00 (두 개를 합쳐야 덮이는 방문도 허용)
    private List<int[]> mergedBlocks(List<Availability> availabilities, Set<String> skipped, LocalDate day) {
        List<int[]> intervals = availabilities.stream()
                .filter(a -> a.isScheduledOn(day))
                .filter(a -> !skipped.contains(key(a.getId(), day)))
                .map(a -> a.isAllDay()
                        ? new int[]{0, DAY_SECONDS}
                        : new int[]{a.getStartTime().toSecondOfDay(), a.getEndTime().toSecondOfDay()})
                .sorted(Comparator.comparingInt(i -> i[0]))
                .toList();

        List<int[]> merged = new ArrayList<>();
        for (int[] interval : intervals) {
            if (!merged.isEmpty() && interval[0] <= merged.getLast()[1]) {
                merged.getLast()[1] = Math.max(merged.getLast()[1], interval[1]);
            } else {
                merged.add(new int[]{interval[0], interval[1]});
            }
        }
        return merged;
    }

    // [from, to]를 통째로 품는 블록
    private Optional<int[]> coveringBlock(List<int[]> blocks, int from, int to) {
        return blocks.stream().filter(b -> b[0] <= from && to <= b[1]).findFirst();
    }

    // 방문 시작일 이후의 해제 날짜를 한 번에 조회 (가용시간마다 따로 조회하지 않음)
    private Set<String> loadSkipped(List<Availability> availabilities, LocalDate from) {
        return availabilityExceptionRepository
                .findByAvailabilityIdInAndSkipDateGreaterThanEqualOrderBySkipDate(
                        availabilities.stream().map(Availability::getId).toList(), from)
                .stream()
                .map(e -> key(e.getAvailability().getId(), e.getSkipDate()))
                .collect(Collectors.toSet());
    }

    private String key(Long availabilityId, LocalDate date) {
        return availabilityId + "@" + date;
    }

    // 36번 응답용 가용 구간. 공용 면은 allDay false + 시각 없음 (명세)
    public record Window(boolean common, boolean allDay, LocalTime startTime, LocalTime endTime) {

        static final Window COMMON = new Window(true, false, null, null);

        static Window of(int[] block) {
            if (block[0] == 0 && block[1] == DAY_SECONDS) {
                return new Window(false, true, null, null);
            }
            return new Window(false, false, LocalTime.ofSecondOfDay(block[0]),
                    LocalTime.ofSecondOfDay(block[1])); // 끝이 24:00인 블록은 종일(0~24시)뿐이라 위에서 걸러짐
        }
    }
}

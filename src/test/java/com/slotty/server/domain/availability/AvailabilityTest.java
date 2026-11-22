package com.slotty.server.domain.availability;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

// 가용시간 회차 판정 (입차 8단계). DB·스프링 없이 엔티티 메서드만 검사
class AvailabilityTest {

    private static final int WEEKDAYS = 31; // 월~금 0011111
    private static final int WEEKEND = 96;  // 토·일 1100000
    private static final int SUNDAY = 64;

    // 2026-11-23 월요일, 2026-11-28 토요일, 2026-11-29 일요일
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 23);
    private static final LocalDate SATURDAY = LocalDate.of(2026, 11, 28);
    private static final LocalDate SUNDAY_DATE = LocalDate.of(2026, 11, 29);

    @Test
    @DisplayName("평일 09~18시: 월요일 10시는 열림, 토요일 10시는 닫힘")
    void weekdaysOnly() {
        Availability weekdayDaytime = recurring(WEEKDAYS, false, "09:00", "18:00");

        assertThat(weekdayDaytime.isOpenAt(MONDAY.atTime(10, 0))).isTrue();
        assertThat(weekdayDaytime.isOpenAt(SATURDAY.atTime(10, 0))).isFalse();
    }

    @Test
    @DisplayName("시작 시각은 포함, 끝 시각은 제외")
    void startInclusiveEndExclusive() {
        Availability weekdayDaytime = recurring(WEEKDAYS, false, "09:00", "18:00");

        assertThat(weekdayDaytime.isOpenAt(MONDAY.atTime(8, 59, 59))).isFalse();
        assertThat(weekdayDaytime.isOpenAt(MONDAY.atTime(9, 0))).isTrue();
        assertThat(weekdayDaytime.isOpenAt(MONDAY.atTime(17, 59, 59))).isTrue();
        assertThat(weekdayDaytime.isOpenAt(MONDAY.atTime(18, 0))).isFalse();
    }

    @Test
    @DisplayName("주말 종일: 일요일 23:59도 열림, 월요일은 닫힘")
    void weekendAllDay() {
        Availability weekend = recurring(WEEKEND, true, null, null);

        assertThat(weekend.isOpenAt(SUNDAY_DATE.atTime(23, 59))).isTrue();
        assertThat(weekend.isOpenAt(SATURDAY.atTime(0, 0))).isTrue();
        assertThat(weekend.isOpenAt(MONDAY.atTime(12, 0))).isFalse();
    }

    @Test
    @DisplayName("일요일 비트(64): 요일 값 7 → 1 << 6")
    void sundayBit() {
        Availability sundayOnly = recurring(SUNDAY, true, null, null);

        assertThat(sundayOnly.isOpenAt(SUNDAY_DATE.atTime(12, 0))).isTrue();
        assertThat(sundayOnly.isOpenAt(SATURDAY.atTime(12, 0))).isFalse();
    }

    @Test
    @DisplayName("반복이 아니면 그 날짜에만 열림")
    void oneTime() {
        Availability once = oneTime(SATURDAY, "13:00", "15:00");

        assertThat(once.isOpenAt(SATURDAY.atTime(14, 0))).isTrue();
        assertThat(once.isOpenAt(SATURDAY.plusWeeks(1).atTime(14, 0))).isFalse();
        assertThat(once.isOpenAt(SATURDAY.atTime(15, 0))).isFalse();
    }

    // 같은 패키지라 protected 생성자 사용 가능. 필드는 setter가 없어 ReflectionTestUtils로 채움
    private Availability recurring(int daysMask, boolean allDay, String start, String end) {
        Availability availability = new Availability();
        ReflectionTestUtils.setField(availability, "recurring", true);
        ReflectionTestUtils.setField(availability, "daysMask", daysMask);
        setTime(availability, allDay, start, end);
        return availability;
    }

    private Availability oneTime(LocalDate date, String start, String end) {
        Availability availability = new Availability();
        ReflectionTestUtils.setField(availability, "recurring", false);
        ReflectionTestUtils.setField(availability, "targetDate", date);
        setTime(availability, false, start, end);
        return availability;
    }

    private void setTime(Availability availability, boolean allDay, String start, String end) {
        ReflectionTestUtils.setField(availability, "allDay", allDay);
        ReflectionTestUtils.setField(availability, "startTime", start == null ? null : LocalTime.parse(start));
        ReflectionTestUtils.setField(availability, "endTime", end == null ? null : LocalTime.parse(end));
    }
}

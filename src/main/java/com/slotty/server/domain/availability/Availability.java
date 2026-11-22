package com.slotty.server.domain.availability;

import com.slotty.server.domain.slot.ParkingSlot;
import com.slotty.server.domain.user.User;
import com.slotty.server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "availability")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Availability extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id")
    private ParkingSlot slot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registered_by")
    private User registeredBy;

    @Column(name = "is_recurring", nullable = false)
    private boolean recurring;

    @JdbcTypeCode(SqlTypes.TINYINT)
    private Integer daysMask; // 월=1 화=2 수=4 목=8 금=16 토=32 일=64 (반복일 때만)

    private LocalDate targetDate; // 반복이 아닐 때의 날짜

    @Column(nullable = false)
    private boolean allDay;

    private LocalTime startTime; // 종일이면 null
    private LocalTime endTime;

    // 이 시각이 가용시간 회차 안인가 (입차 8단계)
    public boolean isOpenAt(LocalDateTime at) {
        if (recurring) {
            int todayBit = 1 << (at.getDayOfWeek().getValue() - 1); // 월요일(1) → 1, 일요일(7) → 64
            if ((daysMask & todayBit) == 0) {
                return false;
            }
        } else if (!targetDate.equals(at.toLocalDate())) {
            return false;
        }

        if (allDay) {
            return true;
        }
        LocalTime time = at.toLocalTime();
        return !time.isBefore(startTime) && time.isBefore(endTime); // 시작 포함, 끝 제외
    }

    // 매주 반복 (days_mask: 월=1 ... 일=64)
    public static Availability recurring(ParkingSlot slot, User registeredBy, int daysMask,
                                         boolean allDay, LocalTime startTime, LocalTime endTime) {
        Availability availability = base(slot, registeredBy, allDay, startTime, endTime);
        availability.recurring = true;
        availability.daysMask = daysMask;
        return availability;
    }

    // 하루만
    public static Availability once(ParkingSlot slot, User registeredBy, LocalDate targetDate,
                                    boolean allDay, LocalTime startTime, LocalTime endTime) {
        Availability availability = base(slot, registeredBy, allDay, startTime, endTime);
        availability.recurring = false;
        availability.targetDate = targetDate;
        return availability;
    }

    private static Availability base(ParkingSlot slot, User registeredBy,
                                     boolean allDay, LocalTime startTime, LocalTime endTime) {
        Availability availability = new Availability();
        availability.slot = slot;
        availability.registeredBy = registeredBy;
        availability.allDay = allDay;
        availability.startTime = allDay ? null : startTime;
        availability.endTime = allDay ? null : endTime;
        return availability;
    }

    // 그 날짜에 원래 회차가 있는가 ([이번만 해제] 검증: 반복이면 요일, 단건이면 날짜)
    public boolean isScheduledOn(LocalDate date) {
        if (recurring) {
            int dayBit = 1 << (date.getDayOfWeek().getValue() - 1);
            return (daysMask & dayBit) != 0;
        }
        return targetDate.equals(date);
    }
}

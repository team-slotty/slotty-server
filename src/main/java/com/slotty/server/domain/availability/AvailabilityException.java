package com.slotty.server.domain.availability;

import com.slotty.server.domain.user.User;
import com.slotty.server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// 자바 예외가 아닌 엔티티입니다. (가용시간 예외 도메인)
@Entity
@Table(name = "availability_exception")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AvailabilityException extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "availability_id")
    private Availability availability;

    @Column(nullable = false)
    private LocalDate skipDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;

    // [이번만 해제] 또는 주인 입차 시 자동 해제
    public static AvailabilityException skip(Availability availability, LocalDate skipDate, User createdBy) {
        AvailabilityException exception = new AvailabilityException();
        exception.availability = availability;
        exception.skipDate = skipDate;
        exception.createdBy = createdBy;
        return exception;
    }
}

package com.slotty.server.domain.parking;

import com.slotty.server.domain.slot.ParkingSlot;
import com.slotty.server.domain.vehicle.Vehicle;
import com.slotty.server.domain.user.User;
import com.slotty.server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "parking_record")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 생성자를 통해서 값 변경 목적으로 접근하는 메시지들 차단
public class ParkingRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id")
    private ParkingSlot slot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @Column(nullable = false, length = 36, unique = true)
    private String clientRequestId;

    @Column(nullable = false)
    private LocalDateTime enteredAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entered_by")
    private User enteredBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EnterMethod enterMethod;

    private LocalDateTime exitedAt; // null이면 주차 중

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exited_by")
    private User exitedBy;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private ExitMethod exitMethod;
}

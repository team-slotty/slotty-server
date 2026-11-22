package com.slotty.server.domain.visit;

import com.slotty.server.domain.villa.Household;
import com.slotty.server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 자주 오는 방문차 (세대당 최근 5대만 유지, 방문 등록 때 서비스가 관리)
@Entity
@Table(name = "frequent_vehicle")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FrequentVehicle extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id")
    private Household household;

    @Column(nullable = false, length = 12)
    private String plateNumber;

    @Column(nullable = false)
    private LocalDateTime lastVisitedAt;

    public static FrequentVehicle create(Household household, String plateNumber, LocalDateTime visitedAt) {
        FrequentVehicle frequentVehicle = new FrequentVehicle();
        frequentVehicle.household = household;
        frequentVehicle.plateNumber = plateNumber;
        frequentVehicle.lastVisitedAt = visitedAt;
        return frequentVehicle;
    }

    // 같은 차를 다시 등록하면 새 행 대신 시각만 갱신 (uk_frequent_household_plate)
    public void touch(LocalDateTime visitedAt) {
        this.lastVisitedAt = visitedAt;
    }
}

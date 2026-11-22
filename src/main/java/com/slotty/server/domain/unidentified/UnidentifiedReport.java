package com.slotty.server.domain.unidentified;

import com.slotty.server.domain.slot.ParkingSlot;
import com.slotty.server.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "unidentified_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UnidentifiedReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id")
    private ParkingSlot slot;

    @Column(length = 12)
    private String plateNumber; // 모르면 null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reported_by")
    private User reportedBy;

    @Column(nullable = false)
    private LocalDateTime reportedAt;

    private LocalDateTime resolvedAt; // null이면 미해제

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private ResolveType resolveType;

    // 43. 기록 (번호를 모르면 plateNumber null)
    public static UnidentifiedReport report(ParkingSlot slot, String plateNumber, User reportedBy, LocalDateTime reportedAt) {
        UnidentifiedReport report = new UnidentifiedReport();
        report.slot = slot;
        report.plateNumber = plateNumber;
        report.reportedBy = reportedBy;
        report.reportedAt = reportedAt;
        return report;
    }

    // 44. [차량 떠남] LEFT, 45. [내 차예요] MINE: 해제한 사람을 남김
    public void resolve(User resolvedBy, ResolveType resolveType, LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
        this.resolvedBy = resolvedBy;
        this.resolveType = resolveType;
    }

    public boolean isOpen() {
        return resolvedAt == null;
    }

    // 정상 입차로 자동 해제 (입차 7단계). 해제한 사람이 없으므로 resolvedBy는 null
    public void resolveAuto(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
        this.resolvedBy = null;
        this.resolveType = ResolveType.AUTO;
    }
}

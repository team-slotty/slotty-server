package com.slotty.server.domain.slot;

import com.slotty.server.domain.villa.Household;
import com.slotty.server.domain.villa.Villa;
import com.slotty.server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "parking_slot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 생성자를 통해서 값 변경 목적으로 접근하는 메시지들 차단
public class ParkingSlot extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "villa_id")
    private Villa villa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id")
    private Household household; // null이면 공용 면

    @Column(nullable = false, length = 10)
    private String label;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal x;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal y;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal w;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal h;

    @Column(nullable = false)
    private short rotation;
}

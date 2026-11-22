package com.slotty.server.domain.vehicle;

import com.slotty.server.domain.villa.Household;
import com.slotty.server.domain.user.User;
import com.slotty.server.domain.villa.Villa;
import com.slotty.server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "vehicle")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 생성자를 통해서 값 변경 목적으로 접근하는 메시지들 차단
public class Vehicle extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "villa_id")
    private Villa villa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id")
    private Household household;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_user_id")
    private User contactUser;

    @Column(nullable = false, length = 12)
    private String plateNumber;

    @Column(nullable = false, length = 4)
    private String plateLast4;

    // 차량 등록: 뒤 4자리(검색용)는 서버가 계산
    public static Vehicle register(Villa villa, Household household, User contactUser, String plateNumber) {
        Vehicle vehicle = new Vehicle();
        vehicle.villa = villa;
        vehicle.household = household;
        vehicle.contactUser = contactUser;
        vehicle.changePlateNumber(plateNumber);
        return vehicle;
    }

    // 번호판을 바꾸면 뒤 4자리도 같이 (둘이 어긋나면 차량번호 검색이 틀어짐)
    public void changePlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
        this.plateLast4 = plateNumber.substring(plateNumber.length() - 4);
    }

    public void changeContactUser(User contactUser) {
        this.contactUser = contactUser;
    }
}

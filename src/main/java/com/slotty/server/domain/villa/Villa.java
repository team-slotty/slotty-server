package com.slotty.server.domain.villa;

import com.slotty.server.domain.user.User;
import com.slotty.server.global.entity.BaseTimeEntity;
import com.slotty.server.global.external.Coordinates;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "villa")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 생성자를 통해서 값 변경 목적으로 접근하는 메시지들 차단
public class Villa extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_user_id")
    private User adminUser; // 관리자

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 200)
    private String address;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude; // 위도

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude; // 경도

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LayoutTemplate layoutTemplate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EntranceSide entranceSide;

    // 관리자 위임 (빌라당 관리자 1명)
    public void changeAdmin(User newAdmin) {
        this.adminUser = newAdmin;
    }

    // 빌라 온보딩: 관리자는 세대·사용자를 만든 뒤 마지막에 지정 (villa ↔ users 순환 참조)
    public static Villa create(String name, String address, Coordinates anchor,
                               LayoutTemplate layoutTemplate, EntranceSide entranceSide) {
        Villa villa = new Villa();
        villa.name = name;
        villa.address = address;
        villa.changeAnchor(anchor);
        villa.layoutTemplate = layoutTemplate;
        villa.entranceSide = entranceSide;
        return villa;
    }

    public void changeName(String name) {
        this.name = name;
    }

    // 주소가 바뀌면 기준점도 같이 (변환 실패면 null → 도착·출발 알림 비활성)
    public void changeAddress(String address, Coordinates anchor) {
        this.address = address;
        changeAnchor(anchor);
    }

    // 위도·경도는 항상 함께 있거나 함께 없음 (ck_villa_coords)
    public void changeAnchor(Coordinates anchor) {
        this.latitude = anchor == null ? null : anchor.latitude();
        this.longitude = anchor == null ? null : anchor.longitude();
    }

    public void changeLayout(LayoutTemplate layoutTemplate, EntranceSide entranceSide) {
        this.layoutTemplate = layoutTemplate;
        this.entranceSide = entranceSide;
    }
}

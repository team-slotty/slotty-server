package com.slotty.server.domain.villa;

import com.slotty.server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "household")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Household extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) // 세대 여러 개가 빌라 하나에 속함
    @JoinColumn(name = "villa_id") // 연결 저장하는 FK 컬럼
    private Villa villa; // 객체와 테이블의 연관관계 매핑

    @Column(nullable = false, length = 10)
    private String unitNumber;

    @Column(nullable = false, length = 12, unique = true)
    private String inviteCode;
}

package com.slotty.server.domain.user;

import com.slotty.server.domain.villa.Household;
import com.slotty.server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 생성자를 통해서 값 변경 목적으로 접근하는 메시지들 차단
// 아무런 매개변수가 없는 생성자를 생성하되, 다른 패키지에 소속된 클래스는 접근을 불허한다.
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id")
    private Household household;

    @Column(nullable = false, length = 30, unique = true)
    private String loginId;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false)
    private boolean phonePublic;

    private LocalDateTime deletedAt;

    // 회원가입: 가입 직후에는 세대 소속이 없음 (household = null → 빌라 만들기 또는 초대 코드 참여)
    public static User create(String loginId, String passwordHash, String name, String phone, boolean phonePublic) {
        User user = new User();
        user.loginId = loginId;
        user.passwordHash = passwordHash;
        user.name = name;
        user.phone = phone;
        user.phonePublic = phonePublic;
        return user;
    }

    // 초대 코드 참여: 세대에 소속
    public void joinHousehold(Household household) {
        this.household = household;
    }

    // 세대원 내보내기: 계정은 유지하고 소속만 해제
    public void leaveHousehold() {
        this.household = null;
    }

    // 6. 내 정보 수정: 보낸 값만 (null이면 그대로)
    public void updateProfile(String name, String phone, Boolean phonePublic) {
        if (name != null) {
            this.name = name;
        }
        if (phone != null) {
            this.phone = phone;
        }
        if (phonePublic != null) {
            this.phonePublic = phonePublic;
        }
    }

    // 7. 탈퇴: 행은 지우지 않고(입출차·방문 기록의 FK) 개인정보만 지움 (소프트 삭제)
    public void withdraw(LocalDateTime at) {
        this.deletedAt = at;
        this.loginId = WITHDRAWN_LOGIN_ID_PREFIX + id; // 같은 아이디로 다시 가입할 수 있게 비움 (uk_users_login_id)
        this.name = WITHDRAWN_NAME;
        this.phone = "";                               // NOT NULL 컬럼이라 빈 값
        this.phonePublic = false;
        this.passwordHash = WITHDRAWN_PASSWORD_HASH;   // BCrypt 형식이 아니라 어떤 비밀번호와도 일치하지 않음
        this.household = null;
    }

    public static final String WITHDRAWN_LOGIN_ID_PREFIX = "deleted_";
    public static final String WITHDRAWN_NAME = "탈퇴한 사용자";
    private static final String WITHDRAWN_PASSWORD_HASH = "!withdrawn";
}

package com.slotty.server.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 현황판 진입용: 사용자 -> 세대 -> 빌라를 한 번에 조회
    // inner join이라 세대에 소속되지 않은 사용자는 결과에서 빠짐 (Optional.empty -> 현황판 접근 불가)
    // deletedAt is null로 탈퇴한 사용자 차단
    @Query("""
            select u from User u
            join fetch u.household h
            join fetch h.villa
            where u.id = :userId and u.deletedAt is null
            """)

    Optional<User> findActiveWithHouseholdAndVilla(@Param("userId") Long userId);

    // 로그인용: 탈퇴하지 않은 사용자를 로그인 ID로 조회
    Optional<User> findByLoginIdAndDeletedAtIsNull(String loginId);

    // 재발급용: 탈퇴하지 않은 사용자인지 확인
    // 로그인 후 14일 안에 탈퇴한 사용자는 Refresh가 Redis에 남아 있어도 재발급을 막아야 함
    boolean existsByIdAndDeletedAtIsNull(Long id);

    // 회원가입 아이디 중복 확인 (탈퇴 계정은 login_id가 deleted_{id}로 바뀌므로 전체에서 확인)
    boolean existsByLoginId(String loginId);

    // 내 정보: 세대·빌라가 없을 수도 있어 left join (inner join이면 가입 직후 사용자가 결과에서 빠짐)
    @Query("""
            select u from User u
            left join fetch u.household h
            left join fetch h.villa
            where u.id = :userId and u.deletedAt is null
            """)
    Optional<User> findActiveWithOptionalHousehold(@Param("userId") Long userId);

    // 내 세대의 세대원 (탈퇴한 사용자 제외)
    List<User> findByHouseholdIdAndDeletedAtIsNullOrderById(Long householdId);

    // 연락 담당자 지정: 내 세대의 활성 사용자일 때만
    Optional<User> findByIdAndHouseholdIdAndDeletedAtIsNull(Long id, Long householdId);

    // 관리자 세대 목록의 세대원: 빌라 전체 활성 사용자 + 세대 (세대별로 묶기 위해 fetch join)
    @Query("""
            select u from User u
            join fetch u.household h
            where h.villa.id = :villaId and u.deletedAt is null
            order by u.id
            """)
    List<User> findActiveMembersByVillaId(@Param("villaId") Long villaId);

    // 세대 삭제 전 가입자 확인
    boolean existsByHouseholdId(Long householdId);
}

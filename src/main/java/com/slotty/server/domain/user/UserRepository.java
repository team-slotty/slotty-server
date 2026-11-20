package com.slotty.server.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}

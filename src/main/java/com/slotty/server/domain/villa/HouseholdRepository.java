package com.slotty.server.domain.villa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HouseholdRepository extends JpaRepository<Household, Long> {

    // 초대 코드로 세대 찾기 + 빌라 (응답에 빌라 이름이 필요해서 fetch join)
    @Query("""
            select h from Household h
            join fetch h.villa
            where h.inviteCode = :inviteCode
            """)
    Optional<Household> findWithVillaByInviteCode(@Param("inviteCode") String inviteCode);

    // 관리자 세대 목록
    List<Household> findByVillaIdOrderById(Long villaId);

    // 우리 빌라 세대일 때만 (다른 빌라면 empty → 404)
    Optional<Household> findByIdAndVillaId(Long id, Long villaId);

    // 같은 빌라 호수 중복 (uk_household_villa_unit)
    boolean existsByVillaIdAndUnitNumber(Long villaId, String unitNumber);

    // 초대 코드 중복 (uk_household_invite_code)
    boolean existsByInviteCode(String inviteCode);

    // 빌라 정보의 세대 수
    long countByVillaId(Long villaId);
}

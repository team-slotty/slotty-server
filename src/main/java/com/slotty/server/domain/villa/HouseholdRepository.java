package com.slotty.server.domain.villa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface HouseholdRepository extends JpaRepository<Household, Long> {

    // 초대 코드로 세대 찾기 + 빌라 (응답에 빌라 이름이 필요해서 fetch join)
    @Query("""
            select h from Household h
            join fetch h.villa
            where h.inviteCode = :inviteCode
            """)
    Optional<Household> findWithVillaByInviteCode(@Param("inviteCode") String inviteCode);
}

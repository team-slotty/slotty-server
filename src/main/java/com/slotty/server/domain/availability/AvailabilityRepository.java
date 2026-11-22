package com.slotty.server.domain.availability;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AvailabilityRepository extends JpaRepository<Availability, Long> {

    // 이 면에 등록된 가용시간 전체 (회차 판정은 엔티티의 isOpenAt)
    List<Availability> findBySlotId(Long slotId);

    // 목록: 내 세대 배정 면의 가용시간 + 면 (slotId가 null이면 전체 배정 면)
    @Query("""
            select a from Availability a
            join fetch a.slot s
            where s.household.id = :householdId
              and (:slotId is null or s.id = :slotId)
            order by s.id, a.id
            """)
    List<Availability> findWithSlotByHouseholdId(@Param("householdId") Long householdId, @Param("slotId") Long slotId);

    // 삭제·해제: 가용시간 + 면 (권한 확인에 면의 빌라·세대가 필요)
    @Query("select a from Availability a join fetch a.slot where a.id = :id")
    Optional<Availability> findWithSlotById(@Param("id") Long id);

    // 면 주인이 바뀌거나 공용이 될 때 그 면의 가용시간 전부 삭제
    @Modifying
    @Query("delete from Availability a where a.slot.id = :slotId")
    void deleteAllBySlotId(@Param("slotId") Long slotId);
}

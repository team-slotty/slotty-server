package com.slotty.server.domain.slot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, Long> {

    // 빌라의 주차면 전체 + 배정 세대 (assignedUnitNumber, isMine 판단용)
    // 공용 면은 household가 null이라 left join (inner join이면 공용 면이 결과에서 사라짐)
    // label은 문자열이라 "10번"이 "2번"보다 앞에 오므로 id로 정렬
    @Query("""
            select s from ParkingSlot s
            left join fetch s.household
            where s.villa.id = :villaId
            order by s.id
            """)
    List<ParkingSlot> findAllWithHouseholdByVillaId(@Param("villaId") Long villaId);

    // 우리 빌라 면일 때만 찾음 (다른 빌라 면이면 empty → 404)
    Optional<ParkingSlot> findByIdAndVillaId(Long id, Long villaId);

    // 내 정보의 assignedSlotIds: 엔티티 전체가 아니라 id만 필요해서 id만 조회
    @Query("select s.id from ParkingSlot s where s.household.id = :householdId order by s.id")
    List<Long> findIdsByHouseholdId(@Param("householdId") Long householdId);

    // 내 세대 배정 면 (내 세대 화면)
    List<ParkingSlot> findByHouseholdIdOrderById(Long householdId);

    // 참여 응답의 hasAssignedSlot: 개수가 아니라 "있나?"만 필요해서 exists
    boolean existsByHouseholdId(Long householdId);
}

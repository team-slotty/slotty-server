package com.slotty.server.domain.slot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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
}

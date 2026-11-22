package com.slotty.server.domain.vehicle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    // 내 세대 차량 (myVehicles). 연관 엔티티를 안 쓰므로 메서드 이름 쿼리로 충분
    List<Vehicle> findByHouseholdIdOrderById(Long householdId);

    // 내 세대 차량일 때만 찾음 (다른 세대 차량이면 empty → 404)
    Optional<Vehicle> findByIdAndHouseholdId(Long id, Long householdId);

    // 차량 목록: 연락 담당자 이름이 필요해서 fetch join (담당자 없는 차도 나와야 해서 left)
    @Query("""
            select v from Vehicle v
            left join fetch v.contactUser
            where v.household.id = :householdId
            order by v.id
            """)
    List<Vehicle> findWithContactByHouseholdId(@Param("householdId") Long householdId);

    // 같은 빌라 번호판 중복 (등록)
    boolean existsByVillaIdAndPlateNumber(Long villaId, String plateNumber);

    // 같은 빌라 번호판 중복, 자기 자신 제외 (수정)
    boolean existsByVillaIdAndPlateNumberAndIdNot(Long villaId, String plateNumber, Long id);
}

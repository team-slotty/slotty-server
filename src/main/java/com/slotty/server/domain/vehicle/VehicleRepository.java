package com.slotty.server.domain.vehicle;

import org.springframework.data.domain.Pageable;
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

    // 차량번호 검색: 같은 빌라 + 뒤 4자리 일치(인덱스 idx_vehicle_villa_last4) + 입력값으로 끝나는 번호
    // 호수·연락 담당자를 응답에 쓰므로 fetch join, 최대 건수는 Pageable로 제한
    @Query("""
            select v from Vehicle v
            join fetch v.household
            left join fetch v.contactUser
            where v.villa.id = :villaId
              and v.plateLast4 = :last4
              and v.plateNumber like concat('%', :keyword)
            order by v.id
            """)
    List<Vehicle> searchInVilla(@Param("villaId") Long villaId,
                                @Param("last4") String last4,
                                @Param("keyword") String keyword,
                                Pageable pageable);
}

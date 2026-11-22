package com.slotty.server.domain.parking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ParkingRecordRepository extends JpaRepository<ParkingRecord, Long> {

    // N+1 시연용 (fetch 없음) : 기록마다 차량 / 세대 조회 쿼리가 추가로 나감
    // List<ParkingRecord> findBySlotVillaIdAndExitedAtIsNull(Long villaId);

    // 빌라의 열린 입출차 기록 (exitedAt null = 주차 중) + 차량 + 차량 세대
    // occupant(unitNumber, plateLast4) 표시에 필요해서 fetch join -> 기록 수와 상관없이 쿼리 1번
    // slot은 id만 쓰고 면 정보는 ParkingSlotRepository에서 이미 가져오므로 fetch 안 함
    @Query("""
            select r from ParkingRecord r
            join fetch r.vehicle v
            join fetch v.household
            where r.slot.villa.id = :villaId and r.exitedAt is null
            """)
    List<ParkingRecord> findOpenWithVehicleByVillaId(@Param("villaId") Long villaId);

    // 입차 1단계: 같은 멱등키로 이미 처리된 기록
    Optional<ParkingRecord> findByClientRequestId(String clientRequestId);

    // 이 차량의 열린 기록 (open_vehicle_id 유니크라 최대 1건)
    Optional<ParkingRecord> findByVehicleIdAndExitedAtIsNull(Long vehicleId);

    // 이 면의 열린 기록 (open_slot_id 유니크라 최대 1건)
    Optional<ParkingRecord> findBySlotIdAndExitedAtIsNull(Long slotId);

    // 출차 옵션: 내 세대 차량의 열린 기록 + 차량 + 면 (번호판·면 이름 표시에 둘 다 필요해서 fetch join)
    @Query("""
            select r from ParkingRecord r
            join fetch r.vehicle v
            join fetch r.slot
            where v.household.id = :householdId and r.exitedAt is null
            order by v.id
            """)
    List<ParkingRecord> findOpenWithVehicleAndSlotByHouseholdId(@Param("householdId") Long householdId);

    // 차량 삭제 전 그 차량의 기록 전부 삭제 (DELETE 한 번. 메서드 이름 방식은 한 건씩 조회 후 삭제라 기록 수만큼 쿼리)
    @Modifying
    @Query("delete from ParkingRecord r where r.vehicle.id = :vehicleId")
    void deleteAllByVehicleId(@Param("vehicleId") Long vehicleId);

    // 이력: 내 세대 차량의 최근 기록 (since 이후 입차 + 아직 주차 중인 기록), 최신순, 페이지
    // ManyToOne만 fetch join이라 페이지(LIMIT)가 DB에서 적용됨
    // fetch join이 들어간 쿼리로는 개수를 셀 수 없어서 countQuery를 따로 지정
    @Query(value = """
            select r from ParkingRecord r
            join fetch r.vehicle v
            join fetch r.slot
            join fetch r.enteredBy
            left join fetch r.exitedBy
            where v.household.id = :householdId
              and (r.enteredAt >= :since or r.exitedAt is null)
            order by r.enteredAt desc, r.id desc
            """,
            countQuery = """
            select count(r) from ParkingRecord r
            where r.vehicle.household.id = :householdId
              and (r.enteredAt >= :since or r.exitedAt is null)
            """)
    Page<ParkingRecord> findHistoryByHouseholdId(@Param("householdId") Long householdId,
                                                 @Param("since") LocalDateTime since,
                                                 Pageable pageable);

    // 면 상세: 이 면의 열린 기록 + 차량 + 차량 세대 (호수·번호판 표시)
    @Query("""
            select r from ParkingRecord r
            join fetch r.vehicle v
            join fetch v.household
            where r.slot.id = :slotId and r.exitedAt is null
            """)
    Optional<ParkingRecord> findOpenWithVehicleBySlotId(@Param("slotId") Long slotId);

    // 배치 변경: 삭제할 면에 입출차 기록이 남아 있는지 (FK)
    boolean existsBySlotId(Long slotId);
}

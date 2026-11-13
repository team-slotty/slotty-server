package com.slotty.server.domain.parking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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
}

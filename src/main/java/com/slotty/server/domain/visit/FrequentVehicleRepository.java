package com.slotty.server.domain.visit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FrequentVehicleRepository extends JpaRepository<FrequentVehicle, Long> {

    Optional<FrequentVehicle> findByHouseholdIdAndPlateNumber(Long householdId, String plateNumber);

    // 최근 등록순 전체 (5대를 넘는 오래된 것을 지울 때)
    List<FrequentVehicle> findByHouseholdIdOrderByLastVisitedAtDescIdDesc(Long householdId);

    // 42. 최근 5대
    List<FrequentVehicle> findTop5ByHouseholdIdOrderByLastVisitedAtDescIdDesc(Long householdId);
}

package com.slotty.server.domain.vehicle;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    // 내 세대 차량 (myVehicles). 연관 엔티티를 안 쓰므로 메서드 이름 쿼리로 충분
    List<Vehicle> findByHouseholdIdOrderById(Long householdId);
}

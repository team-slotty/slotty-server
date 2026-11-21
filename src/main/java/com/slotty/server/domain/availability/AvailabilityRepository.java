package com.slotty.server.domain.availability;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AvailabilityRepository extends JpaRepository<Availability, Long> {

    // 이 면에 등록된 가용시간 전체 (회차 판정은 엔티티의 isOpenAt)
    List<Availability> findBySlotId(Long slotId);
}

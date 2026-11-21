package com.slotty.server.domain.availability;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface AvailabilityExceptionRepository extends JpaRepository<AvailabilityException, Long> {

    // 그 날짜가 이미 해제됐는지 (uk_exception_availability_date 위반 방지)
    boolean existsByAvailabilityIdAndSkipDate(Long availabilityId, LocalDate skipDate);
}

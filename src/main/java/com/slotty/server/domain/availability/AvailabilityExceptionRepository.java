package com.slotty.server.domain.availability;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AvailabilityExceptionRepository extends JpaRepository<AvailabilityException, Long> {

    // 그 날짜가 이미 해제됐는지 (uk_exception_availability_date 위반 방지)
    boolean existsByAvailabilityIdAndSkipDate(Long availabilityId, LocalDate skipDate);

    // 목록의 skippedDates: 여러 가용시간의 오늘 이후 해제 날짜를 한 번에 (IN 쿼리)
    List<AvailabilityException> findByAvailabilityIdInAndSkipDateGreaterThanEqualOrderBySkipDate(
            List<Long> availabilityIds, LocalDate from);

    // 가용시간 삭제 전 해제 예외 전부 삭제 (DELETE 한 번)
    @Modifying
    @Query("delete from AvailabilityException e where e.availability.id = :availabilityId")
    void deleteAllByAvailabilityId(@Param("availabilityId") Long availabilityId);

    // 면의 가용시간을 지우기 전, 그 가용시간들의 해제 예외 전부 삭제
    @Modifying
    @Query("delete from AvailabilityException e where e.availability.id in (select a.id from Availability a where a.slot.id = :slotId)")
    void deleteAllBySlotId(@Param("slotId") Long slotId);
}

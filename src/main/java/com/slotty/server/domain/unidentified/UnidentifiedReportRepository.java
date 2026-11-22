package com.slotty.server.domain.unidentified;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UnidentifiedReportRepository extends JpaRepository<UnidentifiedReport, Long> {

    // 이 면의 미해제 미확인 기록 (open_slot_id 유니크라 최대 1건)
    Optional<UnidentifiedReport> findBySlotIdAndResolvedAtIsNull(Long slotId);

    // 현황판: 빌라의 미해제 기록 전부 (면 id → 기록 Map으로 묶어 씀)
    @Query("select r from UnidentifiedReport r where r.slot.villa.id = :villaId and r.resolvedAt is null")
    List<UnidentifiedReport> findOpenByVillaId(@Param("villaId") Long villaId);

    // 배치 변경: 삭제할 면에 미확인 기록이 남아 있는지
    boolean existsBySlotId(Long slotId);

    // 44·45. 해제: 기록 + 면(+면 주인) + 빌라. 같은 기록을 동시에 해제·정정하지 않도록 쓰기 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r from UnidentifiedReport r
            join fetch r.slot s
            left join fetch s.household
            where r.id = :id and s.villa.id = :villaId
            """)
    Optional<UnidentifiedReport> findForUpdateByIdAndVillaId(@Param("id") Long id, @Param("villaId") Long villaId);

    // 46. 이력: 우리 빌라 기록 + 면 + 기록한 사람, 최신순
    // status: null=전체, true=미해제만, false=해제만. 해제 후 7일 지난 기록은 보여주지 않음 (삭제 스케줄러와 같은 기준)
    @Query(value = """
            select r from UnidentifiedReport r
            join fetch r.slot s
            join fetch r.reportedBy
            where s.villa.id = :villaId
              and (r.resolvedAt is null or r.resolvedAt >= :since)
              and (:open is null
                   or (:open = true and r.resolvedAt is null)
                   or (:open = false and r.resolvedAt is not null))
            order by r.reportedAt desc, r.id desc
            """,
            countQuery = """
            select count(r) from UnidentifiedReport r
            where r.slot.villa.id = :villaId
              and (r.resolvedAt is null or r.resolvedAt >= :since)
              and (:open is null
                   or (:open = true and r.resolvedAt is null)
                   or (:open = false and r.resolvedAt is not null))
            """)
    Page<UnidentifiedReport> findHistoryByVillaId(@Param("villaId") Long villaId,
                                                  @Param("open") Boolean open,
                                                  @Param("since") LocalDateTime since,
                                                  Pageable pageable);
}

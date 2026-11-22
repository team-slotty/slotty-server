package com.slotty.server.domain.visit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VisitRepository extends JpaRepository<Visit, Long> {

    // 상세·수정·삭제·종료: 권한 확인에 방문 세대(+빌라)·지정 면(+면 주인)이 필요하고, 응답에 등록자가 필요
    @Query("""
            select v from Visit v
            join fetch v.household h
            join fetch h.villa
            left join fetch v.slot s
            left join fetch s.household
            join fetch v.registeredBy
            where v.id = :id
            """)
    Optional<Visit> findWithDetailsById(@Param("id") Long id);

    // 같은 면에서 시간이 겹치는 다른 방문이 있는가
    // 겹침: 기존 시작 < 새 종료 그리고 기존 끝 > 새 시작. 기존 끝은 [방문 종료]를 눌렀으면 그 시각
    // 면 지정이 취소된 방문은 그 면을 쓰지 않으므로 제외. excludeId는 수정할 때 자기 자신 제외
    @Query("""
            select count(v) from Visit v
            where v.slot.id = :slotId
              and v.slotCancelledAt is null
              and v.startAt < :endAt
              and coalesce(v.endedAt, v.endAt) > :startAt
              and (:excludeId is null or v.id <> :excludeId)
            """)
    long countOverlapping(@Param("slotId") Long slotId,
                          @Param("startAt") LocalDateTime startAt,
                          @Param("endAt") LocalDateTime endAt,
                          @Param("excludeId") Long excludeId);

    // 36. 빌라에서 그 시간에 이미 다른 방문이 지정한 면 id (한 번에 모아 제외)
    @Query("""
            select distinct v.slot.id from Visit v
            where v.slot.villa.id = :villaId
              and v.slotCancelledAt is null
              and v.startAt < :endAt
              and coalesce(v.endedAt, v.endAt) > :startAt
            """)
    List<Long> findOccupiedSlotIds(@Param("villaId") Long villaId,
                                   @Param("startAt") LocalDateTime startAt,
                                   @Param("endAt") LocalDateTime endAt);

    // 가용시간 변경 후 다시 검사할 방문: 이 면을 지정했고, 취소되지 않았고, 아직 끝나지 않은 방문
    @Query("""
            select v from Visit v
            where v.slot.id = :slotId
              and v.slotCancelledAt is null
              and v.endedAt is null
              and v.endAt > :now
            """)
    List<Visit> findActiveBySlotId(@Param("slotId") Long slotId, @Param("now") LocalDateTime now);

    // ===== 37. 우리 세대 방문 목록 (상태 계산 규칙은 Visit.stateAt과 같게) =====

    // UPCOMING = 시작 전 (가까운 순)
    @Query(value = """
            select v from Visit v
            left join fetch v.slot
            join fetch v.registeredBy
            where v.household.id = :householdId
              and v.endedAt is null and v.startAt > :now
            order by v.startAt, v.id
            """,
            countQuery = """
            select count(v) from Visit v
            where v.household.id = :householdId
              and v.endedAt is null and v.startAt > :now
            """)
    Page<Visit> findUpcoming(@Param("householdId") Long householdId, @Param("now") LocalDateTime now, Pageable pageable);

    // ONGOING = 진행 중 + 확인 필요 (종료 시각이 지나도 면 지정이 살아 있으면 [방문 종료] 전까지 여기)
    @Query(value = """
            select v from Visit v
            left join fetch v.slot
            join fetch v.registeredBy
            where v.household.id = :householdId
              and v.endedAt is null and v.startAt <= :now
              and (v.endAt > :now or (v.slot is not null and v.slotCancelledAt is null))
            order by v.startAt, v.id
            """,
            countQuery = """
            select count(v) from Visit v
            where v.household.id = :householdId
              and v.endedAt is null and v.startAt <= :now
              and (v.endAt > :now or (v.slot is not null and v.slotCancelledAt is null))
            """)
    Page<Visit> findOngoing(@Param("householdId") Long householdId, @Param("now") LocalDateTime now, Pageable pageable);

    // PAST = [방문 종료]를 눌렀거나, 면 없이(또는 취소된 면으로) 종료 시각이 지남 (최신순)
    @Query(value = """
            select v from Visit v
            left join fetch v.slot
            join fetch v.registeredBy
            where v.household.id = :householdId
              and (v.endedAt is not null
                   or (v.endAt <= :now and (v.slot is null or v.slotCancelledAt is not null)))
            order by v.startAt desc, v.id desc
            """,
            countQuery = """
            select count(v) from Visit v
            where v.household.id = :householdId
              and (v.endedAt is not null
                   or (v.endAt <= :now and (v.slot is null or v.slotCancelledAt is not null)))
            """)
    Page<Visit> findPast(@Param("householdId") Long householdId, @Param("now") LocalDateTime now, Pageable pageable);

    // 29. 차량번호 검색의 방문차량: 우리 빌라, 종료 안 됨, 진행 중(확인 필요 포함)이거나 24시간 안에 시작
    @Query("""
            select v from Visit v
            join fetch v.household h
            left join fetch v.slot s
            left join fetch s.household
            join fetch v.registeredBy
            where h.villa.id = :villaId
              and v.plateLast4 = :last4
              and v.plateNumber like concat('%', :keyword)
              and v.endedAt is null
              and v.startAt <= :until
              and (v.endAt > :now or (v.slot is not null and v.slotCancelledAt is null))
            order by v.startAt, v.id
            """)
    List<Visit> searchInVilla(@Param("villaId") Long villaId,
                              @Param("last4") String last4,
                              @Param("keyword") String keyword,
                              @Param("now") LocalDateTime now,
                              @Param("until") LocalDateTime until,
                              Pageable pageable);
}

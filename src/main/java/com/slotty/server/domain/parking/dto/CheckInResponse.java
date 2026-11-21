package com.slotty.server.domain.parking.dto;

import com.slotty.server.domain.parking.ParkingRecord;

import java.time.LocalDateTime;

public record CheckInResponse(
        Long recordId,
        Long vehicleId,
        Long slotId,
        String slotLabel,
        LocalDateTime enteredAt,
        boolean ignored,              // 동승자 중복으로 무시하고 기존 기록을 돌려줬으면 true
        Long autoClosedRecordId,      // 출차 누락 기록을 자동 정리했으면 그 기록 id
        boolean unidentifiedResolved, // 이 면의 미확인 기록을 자동 해제했으면 true
        boolean availabilitySkipped   // 내 가용시간 회차를 자동 해제했으면 true
) {
    public static CheckInResponse of(ParkingRecord record, boolean ignored, Long autoClosedRecordId,
                                     boolean unidentifiedResolved, boolean availabilitySkipped) {
        return new CheckInResponse(
                record.getId(),
                record.getVehicle().getId(),
                record.getSlot().getId(),
                record.getSlot().getLabel(),
                record.getEnteredAt(),
                ignored,
                autoClosedRecordId,
                unidentifiedResolved,
                availabilitySkipped
        );
    }

    // 1단계(같은 요청 재전송)·3단계(동승자): 기존 기록을 그대로 돌려줄 때
    public static CheckInResponse existing(ParkingRecord record, boolean ignored) {
        return of(record, ignored, null, false, false);
    }
}

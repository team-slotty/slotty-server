package com.slotty.server.domain.parking.dto;

import com.slotty.server.domain.parking.EnterMethod;
import com.slotty.server.domain.parking.ExitMethod;
import com.slotty.server.domain.parking.ParkingRecord;
import com.slotty.server.domain.user.User;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

// 명세 공통 페이지 형식: { items, page, size, total }
public record HistoryResponse(
        List<HistoryItem> items,
        int page,
        int size,
        long total
) {
    public record HistoryItem(
            Long recordId,
            String plateNumber,
            String slotLabel,
            LocalDateTime enteredAt,
            UserRef enteredBy,
            EnterMethod enterMethod,
            LocalDateTime exitedAt,   // 주차 중이면 null
            UserRef exitedBy,         // 주차 중이거나 AUTO 정리면 null
            ExitMethod exitMethod
    ) {
        public static HistoryItem from(ParkingRecord record) {
            return new HistoryItem(
                    record.getId(),
                    record.getVehicle().getPlateNumber(),
                    record.getSlot().getLabel(),
                    record.getEnteredAt(),
                    UserRef.from(record.getEnteredBy()),
                    record.getEnterMethod(),
                    record.getExitedAt(),
                    UserRef.from(record.getExitedBy()),
                    record.getExitMethod()
            );
        }
    }

    public record UserRef(Long userId, String name) {
        static UserRef from(User user) {
            return user == null ? null : new UserRef(user.getId(), user.getName());
        }
    }

    // Spring Data의 Page → 명세 형식으로 변환
    public static HistoryResponse from(Page<ParkingRecord> page) {
        return new HistoryResponse(
                page.getContent().stream().map(HistoryItem::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }
}

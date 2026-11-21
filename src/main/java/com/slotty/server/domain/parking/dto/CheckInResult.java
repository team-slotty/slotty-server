package com.slotty.server.domain.parking.dto;

// 서비스 → 컨트롤러 전달용. created가 true면 201, false(재전송·동승자)면 200
public record CheckInResult(boolean created, CheckInResponse response) {
}

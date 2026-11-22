package com.slotty.server.domain.unidentified.dto;

import com.slotty.server.domain.unidentified.ResolveType;

import java.time.LocalDateTime;

// 45. [내 차예요] 응답: 해제한 미확인 기록 + 새로 만든 CORRECTION 입차 기록
public record MineResponse(Long reportId, Long recordId, ResolveType resolveType, LocalDateTime enteredAt) {
}

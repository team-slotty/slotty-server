package com.slotty.server.domain.board.event;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

// 현황판이 바뀌었음을 알리는 이벤트 (23번 SSE로 앱에 전달 → 앱이 22번을 다시 호출)
// slotIds가 비어 있으면 "면 구성 자체가 바뀜(배치 변경 등) → 전체 다시 그리기"
public record BoardChangedEvent(Long villaId, List<Long> slotIds) {

    public static BoardChangedEvent of(Long villaId, Long... slotIds) {
        return new BoardChangedEvent(villaId, Arrays.stream(slotIds).filter(Objects::nonNull).distinct().toList());
    }

    public static BoardChangedEvent of(Long villaId, List<Long> slotIds) {
        return new BoardChangedEvent(villaId, slotIds.stream().filter(Objects::nonNull).distinct().toList());
    }

    public static BoardChangedEvent all(Long villaId) {
        return new BoardChangedEvent(villaId, List.of());
    }
}

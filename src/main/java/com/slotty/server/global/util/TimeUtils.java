package com.slotty.server.global.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

// 엔티티는 LocalDateTime이고 응답은 OffsetDateTime이라 변환할 곳이 여러 군데 생긴다.
// 매번 직접 변환하지 않고 한 곳에서 처리하기 위한 시각 변환 유틸임.
// final로 상속 막고, private 생성자로 객체 생성 막음. 정적 메서드만 모아 둔 도구 상자라고 보면 됨.
public final class TimeUtils {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    // private 생성자
    private TimeUtils() {

    }

    // exitedAt, endedAt처럼 null인 값이 많아 null이면 그대로 null 반환
    public static OffsetDateTime toOffset(LocalDateTime time) {
        if (time == null) {
            return null;
        }

        return time.atZone(KST).toOffsetDateTime();
    }

    // serverTime에 사용. 마이크로초 자름
    public static OffsetDateTime now() {
        return OffsetDateTime.now(KST).truncatedTo(ChronoUnit.SECONDS);
    }
}

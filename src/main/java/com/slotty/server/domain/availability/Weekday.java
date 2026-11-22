package com.slotty.server.domain.availability;

import java.util.Arrays;
import java.util.List;

// 요일 (API는 MON~SUN, DB는 days_mask 비트: 월=1 화=2 수=4 목=8 금=16 토=32 일=64)
public enum Weekday {
    MON, TUE, WED, THU, FRI, SAT, SUN;

    // MON(0번째) → 1, SUN(6번째) → 64
    public int bit() {
        return 1 << ordinal();
    }

    // [MON, WED] → 1 + 4 = 5
    public static int toMask(List<Weekday> days) {
        return days.stream().mapToInt(Weekday::bit).reduce(0, (a, b) -> a | b);
    }

    // 5 → [MON, WED]
    public static List<Weekday> fromMask(int mask) {
        return Arrays.stream(values()).filter(day -> (mask & day.bit()) != 0).toList();
    }
}

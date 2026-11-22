package com.slotty.server.domain.slot;

import com.slotty.server.domain.villa.EntranceSide;
import com.slotty.server.domain.villa.LayoutTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

// 배치 템플릿 + 입구 방향 + 면 수 → 각 면의 좌표 (빌라 온보딩 8번, 배치 변경 12번)
// 계산은 "입구가 아래(BOTTOM)" 기준으로 한 번 하고, 다른 입구는 좌표를 돌리거나 뒤집어서 만든다
public final class SlotLayoutCalculator {

    public static final int MIN_SLOTS = 1;
    public static final int MAX_SLOTS = 30;

    private static final double SLOT_DEPTH = 0.30;   // 줄 배치에서 면의 세로 길이
    private static final double MAX_SLOT_WIDTH = 0.20;

    private SlotLayoutCalculator() {
    }

    public static List<SlotPosition> calculate(LayoutTemplate template, EntranceSide entrance, int count) {
        List<double[]> base = switch (template) {          // double[]{x, y, w, h, rotation}
            case LINE -> row(count, 0.05, 0.95, 0.30);
            case DOUBLE -> {
                int top = (count + 1) / 2;                 // 위 줄이 하나 더 많게 (7면 → 4 + 3)
                List<double[]> slots = new ArrayList<>(row(top, 0.05, 0.95, 0.25));
                slots.addAll(row(count - top, 0.05, 0.95, 0.65));
                yield slots;
            }
            case L_SHAPE -> {
                int side = count / 3;                      // 왼쪽 세로 줄에 1/3, 위쪽 가로 줄에 나머지
                List<double[]> slots = new ArrayList<>(row(count - side, 0.25, 0.95, 0.20));
                slots.addAll(column(side, 0.45, 0.95, 0.12));
                yield slots;
            }
        };
        return base.stream().map(slot -> rotateForEntrance(slot, entrance)).map(SlotLayoutCalculator::toPosition).toList();
    }

    // 가로 한 줄: from~to 구간을 k칸으로 나눠 각 칸 중앙에 배치
    private static List<double[]> row(int k, double from, double to, double y) {
        List<double[]> slots = new ArrayList<>();
        double pitch = (to - from) / Math.max(k, 1);
        double width = Math.min(MAX_SLOT_WIDTH, pitch * 0.9);   // 칸 사이 10% 여백
        for (int i = 0; i < k; i++) {
            slots.add(new double[]{from + pitch * (i + 0.5), y, width, SLOT_DEPTH, 0});
        }
        return slots;
    }

    // 세로 한 줄 (L자의 옆 줄): 차가 가로로 서므로 90도
    private static List<double[]> column(int k, double from, double to, double x) {
        List<double[]> slots = new ArrayList<>();
        double pitch = (to - from) / Math.max(k, 1);
        double height = Math.min(MAX_SLOT_WIDTH, pitch * 0.9);
        for (int i = 0; i < k; i++) {
            slots.add(new double[]{x, from + pitch * (i + 0.5), 0.20, height, 90});
        }
        return slots;
    }

    // 입구 방향에 맞게 변환 (기준: 입구 BOTTOM = 차도가 아래, 면이 위쪽)
    private static double[] rotateForEntrance(double[] s, EntranceSide entrance) {
        double x = s[0], y = s[1], w = s[2], h = s[3], rotation = s[4];
        return switch (entrance) {
            case BOTTOM -> s;
            case TOP -> new double[]{x, 1 - y, w, h, rotation};                          // 위아래 뒤집기
            case LEFT -> new double[]{1 - y, x, h, w, (rotation + 90) % 360};           // 입구가 왼쪽 → 면은 오른쪽
            case RIGHT -> new double[]{y, x, h, w, (rotation + 90) % 360};              // 입구가 오른쪽 → 면은 왼쪽
        };
    }

    // DB 컬럼 DECIMAL(5,4)에 맞춰 소수점 4자리
    private static SlotPosition toPosition(double[] s) {
        return new SlotPosition(round(s[0]), round(s[1]), round(s[2]), round(s[3]), (short) s[4]);
    }

    private static BigDecimal round(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }
}

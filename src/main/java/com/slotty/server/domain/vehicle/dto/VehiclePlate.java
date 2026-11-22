package com.slotty.server.domain.vehicle.dto;

// 차량번호 형식 (등록·수정이 같은 규칙을 쓰도록 한 곳에)
public final class VehiclePlate {

    // 지역(한글 0~2자) + 숫자 2~3자리 + 한글 1자 + 숫자 4자리. 사이 공백 허용
    // 예: 12가3456, 123가4567, 서울12가3456, "12가 3456"
    public static final String PATTERN = "^\\s*[가-힣]{0,2}\\s*\\d{2,3}\\s*[가-힣]\\s*\\d{4}\\s*$";
    public static final String MESSAGE = "차량번호 형식이 올바르지 않습니다. (예: 12가3456)";

    private VehiclePlate() {
    }

    // 공백 제거 (저장·중복 확인은 항상 이 값으로)
    public static String normalize(String plateNumber) {
        return plateNumber.replaceAll("\\s", "");
    }
}

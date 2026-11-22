package com.slotty.server.domain.visit.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.slotty.server.domain.vehicle.dto.VehiclePlate;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

// 39. 방문 수정: 보낸 필드만 바뀜
// slotId는 "안 보냄(그대로)"과 "null로 보냄(면 해제)"을 구분해야 해서 record 대신 setter가 있는 클래스로 받음
// Jackson은 JSON에 키가 있으면 값이 null이어도 setter를 부르고, 키가 없으면 부르지 않음 → setter에서 표시
@Getter
@Setter
@NoArgsConstructor
public class VisitUpdateRequest {

    @Pattern(regexp = VehiclePlate.PATTERN, message = VehiclePlate.MESSAGE)
    private String plateNumber;

    private OffsetDateTime startAt;

    private OffsetDateTime endAt;

    private Long slotId;

    @JsonIgnore
    private boolean slotIdPresent; // 요청 JSON에 slotId 키가 있었는가

    public void setSlotId(Long slotId) {
        this.slotId = slotId;
        this.slotIdPresent = true;
    }
}

package com.slotty.server.domain.board.service;

import com.slotty.server.domain.board.VehicleState;
import com.slotty.server.domain.board.dto.MyVehicleResponse;
import com.slotty.server.domain.board.dto.MyVehiclesResponse;
import com.slotty.server.domain.parking.ParkingRecord;
import com.slotty.server.domain.parking.ParkingRecordRepository;
import com.slotty.server.domain.user.User;
import com.slotty.server.domain.user.UserRepository;
import com.slotty.server.domain.vehicle.Vehicle;
import com.slotty.server.domain.vehicle.VehicleRepository;
import com.slotty.server.domain.villa.Household;
import com.slotty.server.domain.villa.Villa;
import com.slotty.server.global.exception.BusinessException;
import com.slotty.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyVehicleService {
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final ParkingRecordRepository parkingRecordRepository;

    public MyVehiclesResponse getMyVehicles(Long userId) {

        // 사용자 찾기: BoardService 1번과 완전히 같습니다.
        // 사용자+세대+빌라를 한 번에 가져오고, 없으면 NOT_IN_HOUSEHOLD 예외.
        User user = userRepository.findActiveWithHouseholdAndVilla(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_IN_HOUSEHOLD)); // 403

        // 세대 꺼내기: 사용자의 getter로 세대를 Household 변수에 담습니다. 호수와 세대 id를 여기서 씁니다.
        Household household = user.getHousehold();
        Villa villa = user.getHousehold().getVilla();

        // 우리 세대 차량 목록: vehicleRepository에 이미 있는 "세대 id로 차량을 id 순서대로" 메서드를 부릅니다.
        // 결과는 Vehicle의 List. seed 기준 101호는 2대입니다.
        List<Vehicle> vehicles = vehicleRepository.findByHouseholdIdOrderById(household.getId());

        // 서 있는 기록: 현황판 3-1과 같은 메서드(findOpenWithVehicleByVillaId)를 빌라 id로 부릅니다.
        List<ParkingRecord> parkingRecords = parkingRecordRepository
                .findOpenWithVehicleByVillaId(villa.getId());

        // 차량 id → 면 id Map: 현황판 3-2와 같은 stream → collect(Collectors.toMap(...))입니다. 키와 값만 다릅니다.
        // 현황판: 키 = 면 id, 값 = 기록 ("이 면에 누가 서 있나")
        // 내 차: 키 = 기록의 차량 id, 값 = 기록의 면 id ("이 차가 어디 서 있나")
        // 그래서 타입은 Map<Long, Long>입니다. 키 람다는 기록 → 차량 → id, 값 람다는 기록 → 면 → id. 둘 다 getter를 두 번 탑니다.
        Map<Long, Long> slotIdByVehicleId = parkingRecords.stream()
                .collect(Collectors.toMap(
                        r -> r.getVehicle().getId(),
                        r -> r.getSlot().getId()
                ));

        // 차 한 대 → 응답 한 줄: 3번 리스트를 stream() → map(...) → toList(). 람다 블록 안에서는 이렇게 합니다.
        // 5번 Map에 지금 차의 id를 넣어 면 id를 꺼냅니다. 주차 중이 아니면 null입니다.
        // MyVehicleResponse의 네 칸:
        //      1. 차량 id
        //      2. 전체 번호판 (내 차니까 전체로)
        //      3. 면 id가 null이면 VehicleState.DRIVING, 아니면 PARKED
        //      4. 면 id가 null이면 null, 아니면 그 면 id를 int로 바꾼 값 (현황판 number와 같은 값)
        List<MyVehicleResponse> vehicleResponses = vehicles.stream()
                .map(vehicle -> {
                    Long slotId = slotIdByVehicleId.get(vehicle.getId()); // 주차 중이 아니면 null
                    return new MyVehicleResponse(
                            vehicle.getId(),
                            vehicle.getPlateNumber(),
                            slotId == null ? VehicleState.DRIVING : VehicleState.PARKED,
                            slotId == null ? null : slotId.intValue()
                    );
                })
                .toList();

        // 반환: MyVehiclesResponse를 만들어 반환합니다. 첫 칸은 2번 세대의 호수, 둘째 칸은 6번 리스트입니다.
        return new MyVehiclesResponse(household.getUnitNumber(), vehicleResponses);
    }
}

package com.slotty.server.domain.vehicle.service;

import com.slotty.server.domain.parking.ParkingRecordRepository;
import com.slotty.server.domain.user.User;
import com.slotty.server.domain.user.UserRepository;
import com.slotty.server.domain.vehicle.Vehicle;
import com.slotty.server.domain.vehicle.VehicleRepository;
import com.slotty.server.domain.vehicle.dto.VehicleCreateRequest;
import com.slotty.server.domain.vehicle.dto.VehiclePlate;
import com.slotty.server.domain.vehicle.dto.VehicleResponse;
import com.slotty.server.domain.vehicle.dto.VehicleUpdateRequest;
import com.slotty.server.domain.villa.Household;
import com.slotty.server.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.slotty.server.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleService {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final ParkingRecordRepository parkingRecordRepository;

    // ===== 25. 내 세대 차량 목록 =====
    public List<VehicleResponse> getVehicles(Long userId) {
        User user = findResident(userId);
        return vehicleRepository.findWithContactByHouseholdId(user.getHousehold().getId())
                .stream()
                .map(VehicleResponse::from)
                .toList();
    }

    // ===== 26. 차량 등록 =====
    @Transactional
    public VehicleResponse register(Long userId, VehicleCreateRequest request) {
        User user = findResident(userId);
        Household household = user.getHousehold();

        String plateNumber = VehiclePlate.normalize(request.plateNumber());
        Long villaId = household.getVilla().getId();
        if (vehicleRepository.existsByVillaIdAndPlateNumber(villaId, plateNumber)) {
            throw new BusinessException(PLATE_DUPLICATED);
        }

        // 담당자를 안 보내면 본인, 보내면 내 세대원인지 확인
        User contact = (request.contactUserId() == null) ? user : findMember(request.contactUserId(), household);

        Vehicle vehicle = Vehicle.register(household.getVilla(), household, contact, plateNumber);
        saveCatchingDuplicate(vehicle);
        return VehicleResponse.from(vehicle);
    }

    // ===== 27. 차량 수정 (보낸 필드만) =====
    @Transactional
    public VehicleResponse update(Long userId, Long vehicleId, VehicleUpdateRequest request) {
        User user = findResident(userId);
        Household household = user.getHousehold();
        Vehicle vehicle = findMyVehicle(vehicleId, household);

        if (request.plateNumber() != null) {
            String plateNumber = VehiclePlate.normalize(request.plateNumber());
            // 자기 자신은 제외하고 중복 확인 (같은 번호로 다시 보내도 409가 나지 않게)
            if (vehicleRepository.existsByVillaIdAndPlateNumberAndIdNot(household.getVilla().getId(), plateNumber, vehicleId)) {
                throw new BusinessException(PLATE_DUPLICATED);
            }
            vehicle.changePlateNumber(plateNumber);
        }
        if (request.contactUserId() != null) {
            vehicle.changeContactUser(findMember(request.contactUserId(), household));
        }

        saveCatchingDuplicate(vehicle);
        return VehicleResponse.from(vehicle);
    }

    // ===== 28. 차량 삭제 =====
    @Transactional
    public void delete(Long userId, Long vehicleId) {
        User user = findResident(userId);
        Vehicle vehicle = findMyVehicle(vehicleId, user.getHousehold());

        // FK(parking_record.vehicle_id) 때문에 기록을 먼저 지워야 차량을 지울 수 있음
        // 주차 중 기록도 함께 지워지므로 그 면은 빈 면이 됨 (명세)
        parkingRecordRepository.deleteAllByVehicleId(vehicle.getId());
        vehicleRepository.delete(vehicle);
    }

    // ===== 공통 =====

    private User findResident(Long userId) {
        return userRepository.findActiveWithHouseholdAndVilla(userId)
                .orElseThrow(() -> new BusinessException(NOT_IN_HOUSEHOLD));
    }

    // 내 세대 차량만 (남의 차량은 404)
    private Vehicle findMyVehicle(Long vehicleId, Household household) {
        return vehicleRepository.findByIdAndHouseholdId(vehicleId, household.getId())
                .orElseThrow(() -> new BusinessException(VEHICLE_NOT_FOUND));
    }

    // 연락 담당자는 내 세대의 활성 사용자만 (다른 세대 사용자는 404)
    private User findMember(Long contactUserId, Household household) {
        return userRepository.findByIdAndHouseholdIdAndDeletedAtIsNull(contactUserId, household.getId())
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND));
    }

    // 동시에 같은 번호로 등록·수정하면 uk_vehicle_villa_plate 위반 → 409
    private void saveCatchingDuplicate(Vehicle vehicle) {
        try {
            vehicleRepository.saveAndFlush(vehicle);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(PLATE_DUPLICATED);
        }
    }
}

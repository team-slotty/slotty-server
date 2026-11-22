package com.slotty.server.domain.parking.service;

import com.slotty.server.domain.availability.Availability;
import com.slotty.server.domain.availability.AvailabilityException;
import com.slotty.server.domain.availability.AvailabilityExceptionRepository;
import com.slotty.server.domain.availability.AvailabilityRepository;
import com.slotty.server.domain.parking.EnterMethod;
import com.slotty.server.domain.parking.ExitMethod;
import com.slotty.server.domain.parking.ParkingRecord;
import com.slotty.server.domain.parking.ParkingRecordRepository;
import com.slotty.server.domain.parking.dto.CheckInRequest;
import com.slotty.server.domain.parking.dto.CheckInResult;
import com.slotty.server.domain.parking.dto.CheckOutRequest;
import com.slotty.server.domain.slot.ParkingSlot;
import com.slotty.server.domain.slot.ParkingSlotRepository;
import com.slotty.server.domain.unidentified.UnidentifiedReportRepository;
import com.slotty.server.domain.user.User;
import com.slotty.server.domain.user.UserRepository;
import com.slotty.server.domain.vehicle.Vehicle;
import com.slotty.server.domain.vehicle.VehicleRepository;
import com.slotty.server.domain.villa.Household;
import com.slotty.server.domain.villa.Villa;
import com.slotty.server.global.exception.BusinessException;
import com.slotty.server.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

// 입차·출차 규칙 단위 테스트: Repository를 Mockito 가짜 객체로 바꿔 DB 없이 서비스 로직만 검사
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // 공통 준비(setUp)를 모든 테스트가 다 쓰지는 않아서
class ParkingServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Mock UserRepository userRepository;
    @Mock VehicleRepository vehicleRepository;
    @Mock ParkingSlotRepository parkingSlotRepository;
    @Mock ParkingRecordRepository parkingRecordRepository;
    @Mock UnidentifiedReportRepository unidentifiedReportRepository;
    @Mock AvailabilityRepository availabilityRepository;
    @Mock AvailabilityExceptionRepository availabilityExceptionRepository;
    @Mock ApplicationEventPublisher eventPublisher; // 현황판 SSE 이벤트 발행 (9번 묶음에서 추가)

    @InjectMocks ParkingService parkingService; // 위 가짜 7개 + 이벤트 발행기를 생성자로 주입

    // 등장인물: 101호(나 id 1, 세대원 id 2), 102호. 면: 내 배정 1번, 102호 배정 2번, 공용 6번. 내 차 id 5
    User me, roommate;
    Household myHousehold, otherHousehold;
    ParkingSlot mySlot, otherSlot, commonSlot;
    Vehicle myCar;

    @BeforeEach
    void setUp() {
        Villa villa = mock(Villa.class);
        when(villa.getId()).thenReturn(1L);

        myHousehold = household(1L, villa);
        otherHousehold = household(2L, villa);

        me = user(1L, myHousehold);
        roommate = user(2L, myHousehold);

        mySlot = slot(1L, "1번", myHousehold);
        otherSlot = slot(2L, "2번", otherHousehold);
        commonSlot = slot(6L, "6번", null);

        myCar = mock(Vehicle.class);
        when(myCar.getId()).thenReturn(5L);

        when(userRepository.findActiveWithHouseholdAndVilla(1L)).thenReturn(Optional.of(me));
        when(userRepository.findActiveWithHouseholdAndVilla(2L)).thenReturn(Optional.of(roommate));
        when(vehicleRepository.findByIdAndHouseholdId(5L, 1L)).thenReturn(Optional.of(myCar));
        when(parkingSlotRepository.findByIdAndVillaId(1L, 1L)).thenReturn(Optional.of(mySlot));
        when(parkingSlotRepository.findByIdAndVillaId(2L, 1L)).thenReturn(Optional.of(otherSlot));
        when(parkingSlotRepository.findByIdAndVillaId(6L, 1L)).thenReturn(Optional.of(commonSlot));
        // 저장하면 받은 객체를 그대로 돌려줌 (진짜 DB처럼)
        when(parkingRecordRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    @DisplayName("입차")
    class CheckIn {

        @Test
        @DisplayName("같은 clientRequestId 재전송이면 새로 만들지 않고 기존 기록을 돌려준다 (200)")
        void replay() {
            ParkingRecord saved = record(100L, commonSlot, me, now().minusMinutes(1));
            when(parkingRecordRepository.findByClientRequestId("uuid-1")).thenReturn(Optional.of(saved));

            CheckInResult result = parkingService.checkIn(1L, request(6L, "uuid-1", null));

            assertThat(result.created()).isFalse();
            assertThat(result.response().recordId()).isEqualTo(100L);
            verify(parkingRecordRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("다른 세대 배정 면이면 403 SLOT_NOT_ALLOWED")
        void otherHouseholdSlot() {
            assertError(() -> parkingService.checkIn(1L, request(2L, "uuid-2", null)), ErrorCode.SLOT_NOT_ALLOWED);
        }

        @Test
        @DisplayName("동승자: 다른 세대원이 5분 안에 같은 차를 입차시켰으면 무시하고 기존 기록 (ignored)")
        void passenger() {
            ParkingRecord byRoommate = record(100L, commonSlot, roommate, now().minusMinutes(2));
            when(parkingRecordRepository.findByVehicleIdAndExitedAtIsNull(5L)).thenReturn(Optional.of(byRoommate));

            CheckInResult result = parkingService.checkIn(1L, request(6L, "uuid-3", null));

            assertThat(result.created()).isFalse();
            assertThat(result.response().ignored()).isTrue();
            assertThat(byRoommate.isOpen()).isTrue(); // 기존 기록은 그대로 열려 있음
            verify(parkingRecordRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("5분이 지났으면 동승자가 아니라 출차 누락 → 옛 기록 AUTO 정리 후 새 입차")
        void notPassengerAfterFiveMinutes() {
            ParkingRecord old = record(100L, commonSlot, roommate, now().minusMinutes(10));
            when(parkingRecordRepository.findByVehicleIdAndExitedAtIsNull(5L)).thenReturn(Optional.of(old));

            CheckInResult result = parkingService.checkIn(1L, request(1L, "uuid-4", null));

            assertThat(result.created()).isTrue();
            assertThat(result.response().autoClosedRecordId()).isEqualTo(100L);
            assertThat(old.getExitMethod()).isEqualTo(ExitMethod.AUTO);
            assertThat(old.getExitedBy()).isNull();
        }

        @Test
        @DisplayName("출차 누락 정리: UPDATE(flush)를 새 기록 INSERT(saveAndFlush)보다 먼저 보낸다")
        void flushBeforeInsert() {
            ParkingRecord mine = record(100L, commonSlot, me, now().minusHours(3)); // 내가 넣은 기록 → 동승자 아님
            when(parkingRecordRepository.findByVehicleIdAndExitedAtIsNull(5L)).thenReturn(Optional.of(mine));

            parkingService.checkIn(1L, request(1L, "uuid-5", null));

            InOrder inOrder = inOrder(parkingRecordRepository);
            inOrder.verify(parkingRecordRepository).flush();
            inOrder.verify(parkingRecordRepository).saveAndFlush(any());
            assertThat(mine.isOpen()).isFalse();
        }

        @Test
        @DisplayName("면에 다른 차가 있으면 409 SLOT_OCCUPIED, 저장하지 않음")
        void slotOccupied() {
            ParkingRecord otherCar = record(200L, commonSlot, roommate, now().minusHours(1));
            when(parkingRecordRepository.findBySlotIdAndExitedAtIsNull(6L)).thenReturn(Optional.of(otherCar));

            assertError(() -> parkingService.checkIn(1L, request(6L, "uuid-6", null)), ErrorCode.SLOT_OCCUPIED);
            verify(parkingRecordRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("동시 입차로 유니크 제약을 위반하면 409 SLOT_OCCUPIED로 바꾼다")
        void concurrentCheckIn() {
            when(parkingRecordRepository.saveAndFlush(any()))
                    .thenThrow(new DataIntegrityViolationException("uk_open_slot"));

            assertError(() -> parkingService.checkIn(1L, request(6L, "uuid-7", null)), ErrorCode.SLOT_OCCUPIED);
        }

        @Test
        @DisplayName("method CORRECTION은 [내 차예요] 전용이라 400")
        void correctionNotAllowed() {
            CheckInRequest correction = new CheckInRequest(5L, 6L, EnterMethod.CORRECTION, "uuid-8", null);

            assertError(() -> parkingService.checkIn(1L, correction), ErrorCode.VALIDATION_FAILED);
        }

        @Test
        @DisplayName("미래 시각은 400, 단 1분 이내(기기 시계 오차)는 허용")
        void futureTime() {
            OffsetDateTime tenMinutesLater = OffsetDateTime.now(KST).plusMinutes(10);
            OffsetDateTime thirtySecondsLater = OffsetDateTime.now(KST).plusSeconds(30);

            assertError(() -> parkingService.checkIn(1L, request(6L, "uuid-9", tenMinutesLater)), ErrorCode.VALIDATION_FAILED);
            assertThat(parkingService.checkIn(1L, request(6L, "uuid-10", thirtySecondsLater)).created()).isTrue();
        }

        @Test
        @DisplayName("내 배정 면 + 지금 가용시간이면 오늘 예외를 만든다, 이미 있으면 만들지 않는다")
        void skipAvailability() {
            Availability open = mock(Availability.class);
            when(open.getId()).thenReturn(7L);
            when(open.isOpenAt(any())).thenReturn(true);
            when(availabilityRepository.findBySlotId(1L)).thenReturn(List.of(open));

            CheckInResult first = parkingService.checkIn(1L, request(1L, "uuid-11", null));
            assertThat(first.response().availabilitySkipped()).isTrue();
            verify(availabilityExceptionRepository).save(any(AvailabilityException.class));

            when(availabilityExceptionRepository.existsByAvailabilityIdAndSkipDate(anyLong(), any())).thenReturn(true);
            CheckInResult second = parkingService.checkIn(1L, request(1L, "uuid-12", null));
            assertThat(second.response().availabilitySkipped()).isFalse();
        }

        @Test
        @DisplayName("공용 면이면 가용시간을 조회하지도 않는다")
        void commonSlotSkipsAvailabilityQuery() {
            parkingService.checkIn(1L, request(6L, "uuid-13", null));

            verify(availabilityRepository, never()).findBySlotId(anyLong());
        }
    }

    @Nested
    @DisplayName("출차")
    class CheckOut {

        @Test
        @DisplayName("정상 출차: 나를 출차자로, 요청한 방식으로 기록을 닫는다")
        void checkOut() {
            ParkingRecord open = record(100L, commonSlot, me, now().minusHours(1));
            when(parkingRecordRepository.findByVehicleIdAndExitedAtIsNull(5L)).thenReturn(Optional.of(open));

            parkingService.checkOut(1L, new CheckOutRequest(5L, ExitMethod.MANUAL, null));

            assertThat(open.isOpen()).isFalse();
            assertThat(open.getExitedBy()).isEqualTo(me);
            assertThat(open.getExitMethod()).isEqualTo(ExitMethod.MANUAL);
        }

        @Test
        @DisplayName("주차 중이 아니면 409 NOT_PARKED (이미 출차한 재전송 포함)")
        void notParked() {
            assertError(() -> parkingService.checkOut(1L, new CheckOutRequest(5L, ExitMethod.MANUAL, null)),
                    ErrorCode.NOT_PARKED);
        }

        @Test
        @DisplayName("method AUTO는 서버 전용이라 400")
        void autoNotAllowed() {
            assertError(() -> parkingService.checkOut(1L, new CheckOutRequest(5L, ExitMethod.AUTO, null)),
                    ErrorCode.VALIDATION_FAILED);
        }

        @Test
        @DisplayName("출차 시각이 입차 시각보다 앞서면 400")
        void exitBeforeEnter() {
            ParkingRecord open = record(100L, commonSlot, me, now().minusHours(1));
            when(parkingRecordRepository.findByVehicleIdAndExitedAtIsNull(5L)).thenReturn(Optional.of(open));
            OffsetDateTime twoHoursAgo = OffsetDateTime.now(KST).minusHours(2);

            assertError(() -> parkingService.checkOut(1L, new CheckOutRequest(5L, ExitMethod.MANUAL, twoHoursAgo)),
                    ErrorCode.VALIDATION_FAILED);
            assertThat(open.isOpen()).isTrue();
        }
    }

    // ===== 준비 도우미 =====

    private static LocalDateTime now() {
        return LocalDateTime.now(KST).withNano(0);
    }

    private static CheckInRequest request(Long slotId, String clientRequestId, OffsetDateTime enteredAt) {
        return new CheckInRequest(5L, slotId, EnterMethod.MANUAL, clientRequestId, enteredAt);
    }

    // 실제 엔티티 메서드(enter)로 만들고, DB가 채워 줄 id만 직접 넣음
    private ParkingRecord record(Long id, ParkingSlot slot, User enteredBy, LocalDateTime enteredAt) {
        ParkingRecord record = ParkingRecord.enter(slot, myCar, enteredBy, EnterMethod.MANUAL, "old-" + id, enteredAt);
        ReflectionTestUtils.setField(record, "id", id);
        return record;
    }

    private static Household household(Long id, Villa villa) {
        Household household = mock(Household.class);
        when(household.getId()).thenReturn(id);
        when(household.getVilla()).thenReturn(villa);
        return household;
    }

    private static User user(Long id, Household household) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getHousehold()).thenReturn(household);
        return user;
    }

    private static ParkingSlot slot(Long id, String label, Household household) {
        ParkingSlot slot = mock(ParkingSlot.class);
        when(slot.getId()).thenReturn(id);
        when(slot.getLabel()).thenReturn(label);
        when(slot.getHousehold()).thenReturn(household);
        return slot;
    }

    private static void assertError(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(expected);
    }
}

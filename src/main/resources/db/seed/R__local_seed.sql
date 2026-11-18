-- 로컬 개발용 테스트 데이터 (local 프로필에서만 실행)
-- R__ = 반복 실행 마이그레이션: 이 파일 내용이 바뀔 때마다 Flyway가 다시 실행한다.
-- 그래서 맨 앞에서 기존 데이터를 지우고 처음부터 다시 넣는다.

-- 1) 지우기: 자식 테이블부터
DELETE FROM unidentified_report;
DELETE FROM availability_exception;
DELETE FROM availability;
DELETE FROM frequent_vehicle;
DELETE FROM visit;
DELETE FROM parking_record;
DELETE FROM vehicle;
DELETE FROM parking_slot;
UPDATE villa SET admin_user_id = NULL;   -- villa ↔ users 순환 참조 끊기
DELETE FROM users;
DELETE FROM household;
DELETE FROM villa;

-- 2) 빌라 → 세대 → 사용자 → 관리자 지정 (온보딩 순서와 같음)
INSERT INTO villa (id, name, address, latitude, longitude, layout_template, entrance_side, created_at) VALUES
    (1, '슬로티빌라', '서울특별시 테스트구 슬로티로 1', 37.5665000, 126.9780000, 'DOUBLE', 'BOTTOM', NOW());

INSERT INTO household (id, villa_id, unit_number, invite_code, created_at) VALUES
                                                                               (1, 1, '101', 'SEED101AAAA', NOW()),
                                                                               (2, 1, '102', 'SEED102BBBB', NOW()),
                                                                               (3, 1, '201', 'SEED201CCCC', NOW()),
                                                                               (4, 1, '202', 'SEED202DDDD', NOW()),
                                                                               (5, 1, '301', 'SEED301EEEE', NOW()),
                                                                               (6, 1, '302', 'SEED302FFFF', NOW());

-- password_hash는 로그인 기능을 만들 때 BCrypt 값으로 바꾼다
INSERT INTO users (id, household_id, login_id, password_hash, name, phone, phone_public, created_at) VALUES
                                                                                                         (1, 1, 'kim101',  'NOT_A_REAL_HASH', '김민준', '010-0000-0001', 1, NOW()),
                                                                                                         (2, 3, 'lee201',  'NOT_A_REAL_HASH', '이서연', '010-0000-0002', 0, NOW()),
                                                                                                         (3, 4, 'park202', 'NOT_A_REAL_HASH', '박지호', '010-0000-0003', 1, NOW()),
                                                                                                         (4, 6, 'choi302', 'NOT_A_REAL_HASH', '최유나', '010-0000-0004', 0, NOW());

UPDATE villa SET admin_user_id = 1 WHERE id = 1;

-- 3) 주차면: 위 4칸(1~4번), 아래 3칸(5~7번). 6·7번은 공용(household_id NULL)
INSERT INTO parking_slot (id, villa_id, household_id, label, x, y, w, h, rotation, created_at) VALUES
                                                                                                   (1, 1, 1,    '1번', 0.1500, 0.2500, 0.2000, 0.3000, 0, NOW()),
                                                                                                   (2, 1, 2,    '2번', 0.3800, 0.2500, 0.2000, 0.3000, 0, NOW()),
                                                                                                   (3, 1, 3,    '3번', 0.6200, 0.2500, 0.2000, 0.3000, 0, NOW()),
                                                                                                   (4, 1, 4,    '4번', 0.8500, 0.2500, 0.2000, 0.3000, 0, NOW()),
                                                                                                   (5, 1, 5,    '5번', 0.1500, 0.6500, 0.2000, 0.3000, 0, NOW()),
                                                                                                   (6, 1, NULL, '6번', 0.3800, 0.6500, 0.2000, 0.3000, 0, NOW()),
                                                                                                   (7, 1, NULL, '7번', 0.6200, 0.6500, 0.2000, 0.3000, 0, NOW());

-- 4) 차량: 101호는 2대
INSERT INTO vehicle (id, villa_id, household_id, contact_user_id, plate_number, plate_last4, created_at) VALUES
                                                                                                             (1, 1, 1, 1, '12가3456', '3456', NOW()),
                                                                                                             (2, 1, 3, 2, '34나5678', '5678', NOW()),
                                                                                                             (3, 1, 4, 3, '56다7890', '7890', NOW()),
                                                                                                             (4, 1, 6, 4, '78라1234', '1234', NOW()),
                                                                                                             (5, 1, 1, 1, '90마2468', '2468', NOW());

-- 5) 입출차 기록: 열린 기록 3건 + 출차 끝난 기록 1건
INSERT INTO parking_record (id, slot_id, vehicle_id, client_request_id, entered_at, entered_by, enter_method,
                            exited_at, exited_by, exit_method, created_at) VALUES
                                                                               (1, 1, 1, '00000000-0000-0000-0000-000000000001', NOW() - INTERVAL 3 HOUR,    1, 'MANUAL',       NULL, NULL, NULL, NOW()),
                                                                               (2, 4, 3, '00000000-0000-0000-0000-000000000002', NOW() - INTERVAL 30 MINUTE, 3, 'NOTIFICATION', NULL, NULL, NULL, NOW()),
                                                                               (3, 7, 4, '00000000-0000-0000-0000-000000000003', NOW() - INTERVAL 1 HOUR,    4, 'NOTIFICATION', NULL, NULL, NULL, NOW()),
                                                                               (4, 3, 2, '00000000-0000-0000-0000-000000000004', NOW() - INTERVAL 26 HOUR,   2, 'MANUAL',
                                                                                NOW() - INTERVAL 20 HOUR, 2, 'MANUAL', NOW());
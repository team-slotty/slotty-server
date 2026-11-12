-- Slotty 초기 스키마 (MySQL 8.4) — ERD v3, 2026-11-11
-- Flyway 첫 마이그레이션: slotty-server/src/main/resources/db/migration/V1__init.sql 로 복사해서 사용
-- 한 번 적용된 뒤에는 이 파일을 고치지 않고, 변경은 V2__..., V3__... 새 파일로 추가한다.
--
-- 설계 원칙
-- 1) "방문 중", "사용 중" 같은 상태는 저장하지 않고 시각 값으로 계산한다
--    (exited_at / ended_at / resolved_at 이 NULL이면 진행 중)
-- 2) 열린 입차 기록(면당·차량당 1건)과 미해제 미확인 기록(면당 1건)은
--    generated column + UNIQUE로 DB가 보장한다 (NULL은 UNIQUE에서 중복 허용)
-- 3) CHECK는 결과가 NULL(UNKNOWN)이면 통과시키므로, NULL이 될 수 있는 컬럼은 IS NOT NULL을 명시한다
-- 4) 빌라 간 데이터 섞임(교차 빌라)은 FK로 막을 수 없어 서비스 인가 검사로 막는다

CREATE TABLE `villa` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '빌라 ID',
  `admin_user_id` BIGINT NULL COMMENT '관리자 (빌라당 1명, 온보딩 트랜잭션 마지막에 지정)',
  `name` VARCHAR(50) NOT NULL COMMENT '빌라 이름',
  `address` VARCHAR(200) NOT NULL COMMENT '주소',
  `latitude` DECIMAL(10,7) NULL COMMENT '기준점 위도 (주소 변환 실패 시 NULL)',
  `longitude` DECIMAL(10,7) NULL COMMENT '기준점 경도',
  `layout_template` VARCHAR(20) NOT NULL COMMENT 'LINE / DOUBLE / L_SHAPE',
  `entrance_side` VARCHAR(10) NOT NULL COMMENT '입구 위치 TOP / BOTTOM / LEFT / RIGHT',
  `created_at` DATETIME NOT NULL COMMENT '생성 시각',
  CONSTRAINT `ck_villa_template` CHECK (`layout_template` IN ('LINE', 'DOUBLE', 'L_SHAPE')),
  CONSTRAINT `ck_villa_entrance` CHECK (`entrance_side` IN ('TOP', 'BOTTOM', 'LEFT', 'RIGHT')),
  CONSTRAINT `ck_villa_coords` CHECK ((`latitude` IS NULL) = (`longitude` IS NULL)),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `household` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '세대 ID',
  `villa_id` BIGINT NOT NULL COMMENT '빌라 ID',
  `unit_number` VARCHAR(10) NOT NULL COMMENT '호수 (예: 201)',
  `invite_code` VARCHAR(12) NOT NULL COMMENT '세대별 초대 코드 (만료 없음, 재발급 시 덮어씀)',
  `created_at` DATETIME NOT NULL COMMENT '생성 시각',
  UNIQUE KEY `uk_household_invite_code` (`invite_code`),
  UNIQUE KEY `uk_household_villa_unit` (`villa_id`, `unit_number`),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '사용자 ID',
  `household_id` BIGINT NULL COMMENT '세대 ID (가입 직후·탈퇴 후 NULL)',
  `login_id` VARCHAR(30) NOT NULL COMMENT '로그인 ID (탈퇴 시 deleted_{id}로 익명화)',
  `password_hash` VARCHAR(100) NOT NULL COMMENT 'BCrypt 해시 (탈퇴 시 무효화)',
  `name` VARCHAR(30) NOT NULL COMMENT '이름',
  `phone` VARCHAR(20) NOT NULL COMMENT '전화번호',
  `phone_public` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '연락처 공개 동의',
  `deleted_at` DATETIME NULL COMMENT '탈퇴 시각 (소프트 삭제)',
  `created_at` DATETIME NOT NULL COMMENT '가입 시각',
  UNIQUE KEY `uk_users_login_id` (`login_id`),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `parking_slot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '주차면 ID',
  `villa_id` BIGINT NOT NULL COMMENT '빌라 ID',
  `household_id` BIGINT NULL COMMENT '배정 세대 ID (NULL이면 공용, 세대당 0개 이상)',
  `label` VARCHAR(10) NOT NULL COMMENT '표시 이름 (예: 1번)',
  `x` DECIMAL(5,4) NOT NULL COMMENT '중심 x (0~1)',
  `y` DECIMAL(5,4) NOT NULL COMMENT '중심 y (0~1)',
  `w` DECIMAL(5,4) NOT NULL COMMENT '너비 (0~1)',
  `h` DECIMAL(5,4) NOT NULL COMMENT '높이 (0~1)',
  `rotation` SMALLINT NOT NULL DEFAULT 0 COMMENT '회전 각도 (0~359)',
  `created_at` DATETIME NOT NULL COMMENT '생성 시각',
  CONSTRAINT `ck_slot_position` CHECK (`x` BETWEEN 0 AND 1 AND `y` BETWEEN 0 AND 1),
  CONSTRAINT `ck_slot_size` CHECK (`w` > 0 AND `w` <= 1 AND `h` > 0 AND `h` <= 1),
  CONSTRAINT `ck_slot_rotation` CHECK (`rotation` BETWEEN 0 AND 359),
  UNIQUE KEY `uk_slot_villa_label` (`villa_id`, `label`),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `vehicle` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '차량 ID',
  `villa_id` BIGINT NOT NULL COMMENT '빌라 ID (빌라 안 번호판 중복 방지·검색 범위용, 세대의 빌라와 같아야 함)',
  `household_id` BIGINT NOT NULL COMMENT '세대 ID',
  `contact_user_id` BIGINT NULL COMMENT '연락 담당자 (담당자 탈퇴·내보내기 후 지정 전까지 NULL)',
  `plate_number` VARCHAR(12) NOT NULL COMMENT '차량번호',
  `plate_last4` CHAR(4) NOT NULL COMMENT '뒤 4자리 (검색용)',
  `created_at` DATETIME NOT NULL COMMENT '등록 시각',
  UNIQUE KEY `uk_vehicle_villa_plate` (`villa_id`, `plate_number`),
  KEY `idx_vehicle_villa_last4` (`villa_id`, `plate_last4`),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `parking_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '입출차 기록 ID',
  `slot_id` BIGINT NOT NULL COMMENT '주차면 ID',
  `vehicle_id` BIGINT NOT NULL COMMENT '차량 ID (입주민 차량만, 방문차는 기록 없음)',
  `client_request_id` VARCHAR(36) NOT NULL COMMENT '입차 요청 멱등키 (앱이 만든 UUID, 재전송 중복 방지)',
  `entered_at` DATETIME NOT NULL COMMENT '입차 시각 (앱에서 탭한 시각)',
  `entered_by` BIGINT NOT NULL COMMENT '입차를 누른 사용자',
  `enter_method` VARCHAR(15) NOT NULL COMMENT 'NOTIFICATION / MANUAL / CORRECTION([내 차예요] 정정)',
  `exited_at` DATETIME NULL COMMENT '출차 시각 (NULL이면 주차 중)',
  `exited_by` BIGINT NULL COMMENT '출차를 누른 사용자 (AUTO면 NULL)',
  `exit_method` VARCHAR(15) NULL COMMENT 'NOTIFICATION / MANUAL / AUTO(출차 누락 자동 정리)',
  `created_at` DATETIME NOT NULL COMMENT '서버 저장 시각',
  `open_slot_id` BIGINT AS (IF(`exited_at` IS NULL, `slot_id`, NULL)) STORED COMMENT '열린 기록이면 slot_id (면당 1건 보장용)',
  `open_vehicle_id` BIGINT AS (IF(`exited_at` IS NULL, `vehicle_id`, NULL)) STORED COMMENT '열린 기록이면 vehicle_id (차량당 1건 보장용)',
  CONSTRAINT `ck_record_enter_method` CHECK (`enter_method` IN ('NOTIFICATION', 'MANUAL', 'CORRECTION')),
  CONSTRAINT `ck_record_exit` CHECK (
    (`exited_at` IS NULL AND `exited_by` IS NULL AND `exit_method` IS NULL)
    OR (`exited_at` IS NOT NULL AND `exited_at` >= `entered_at` AND `exit_method` IS NOT NULL AND `exit_method` IN ('NOTIFICATION', 'MANUAL', 'AUTO'))
  ),
  UNIQUE KEY `uk_record_client_request` (`client_request_id`),
  UNIQUE KEY `uk_record_open_slot` (`open_slot_id`),
  UNIQUE KEY `uk_record_open_vehicle` (`open_vehicle_id`),
  KEY `idx_record_vehicle_entered` (`vehicle_id`, `entered_at`),
  KEY `idx_record_exited` (`exited_at`),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `visit` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '방문 ID',
  `household_id` BIGINT NOT NULL COMMENT '방문 세대 ID',
  `slot_id` BIGINT NULL COMMENT '지정 주차면 (선택)',
  `slot_cancelled_at` DATETIME NULL COMMENT '면 주인이 가용시간을 해제해 면 지정이 취소된 시각 (앱을 열면 방문 세대에 안내)',
  `registered_by` BIGINT NOT NULL COMMENT '등록한 사용자',
  `plate_number` VARCHAR(12) NOT NULL COMMENT '차량번호',
  `plate_last4` CHAR(4) NOT NULL COMMENT '뒤 4자리 (검색용)',
  `start_at` DATETIME NOT NULL COMMENT '방문 시작',
  `end_at` DATETIME NOT NULL COMMENT '방문 종료 예정 (최대 24시간)',
  `ended_at` DATETIME NULL COMMENT '[방문 종료]를 누른 시각',
  `ended_by` BIGINT NULL COMMENT '[방문 종료]를 누른 사용자',
  `created_at` DATETIME NOT NULL COMMENT '등록 시각',
  CONSTRAINT `ck_visit_period` CHECK (`end_at` > `start_at` AND `end_at` <= `start_at` + INTERVAL 24 HOUR),
  CONSTRAINT `ck_visit_cancel` CHECK (`slot_cancelled_at` IS NULL OR `slot_id` IS NOT NULL),
  CONSTRAINT `ck_visit_end` CHECK ((`ended_at` IS NULL) = (`ended_by` IS NULL)),
  KEY `idx_visit_plate_last4` (`plate_last4`),
  KEY `idx_visit_household_start` (`household_id`, `start_at`),
  KEY `idx_visit_slot_start` (`slot_id`, `start_at`),
  KEY `idx_visit_end` (`end_at`),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `frequent_vehicle` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '자주 오는 차량 ID',
  `household_id` BIGINT NOT NULL COMMENT '세대 ID (세대당 최근 5대만 유지, 서비스에서 관리)',
  `plate_number` VARCHAR(12) NOT NULL COMMENT '차량번호',
  `last_visited_at` DATETIME NOT NULL COMMENT '마지막 방문 등록 시각',
  `created_at` DATETIME NOT NULL COMMENT '처음 저장 시각',
  UNIQUE KEY `uk_frequent_household_plate` (`household_id`, `plate_number`),
  KEY `idx_frequent_household_last` (`household_id`, `last_visited_at`),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `availability` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '가용시간 ID',
  `slot_id` BIGINT NOT NULL COMMENT '주차면 ID',
  `registered_by` BIGINT NOT NULL COMMENT '등록한 사용자',
  `is_recurring` TINYINT(1) NOT NULL COMMENT '매주 반복 여부',
  `days_mask` TINYINT UNSIGNED NULL COMMENT '반복 요일 비트마스크 (월=1 화=2 수=4 목=8 금=16 토=32 일=64)',
  `target_date` DATE NULL COMMENT '반복이 아닐 때의 날짜',
  `all_day` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '종일 여부 (1이면 시간 없음)',
  `start_time` TIME NULL COMMENT '시작 시각 (종일이면 NULL, 30분 단위는 앱·서버 검증)',
  `end_time` TIME NULL COMMENT '종료 시각 (자정 넘김 불가)',
  `created_at` DATETIME NOT NULL COMMENT '등록 시각',
  CONSTRAINT `ck_availability_repeat` CHECK (
    (`is_recurring` = 1 AND `days_mask` IS NOT NULL AND `days_mask` BETWEEN 1 AND 127 AND `target_date` IS NULL)
    OR (`is_recurring` = 0 AND `days_mask` IS NULL AND `target_date` IS NOT NULL)
  ),
  CONSTRAINT `ck_availability_time` CHECK (
    (`all_day` = 1 AND `start_time` IS NULL AND `end_time` IS NULL)
    OR (`all_day` = 0 AND `start_time` IS NOT NULL AND `end_time` IS NOT NULL AND `start_time` < `end_time`)
  ),
  KEY `idx_availability_slot` (`slot_id`),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `availability_exception` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '가용시간 예외 ID ([이번만 해제] / 본인 입차 시 자동 해제)',
  `availability_id` BIGINT NOT NULL COMMENT '가용시간 ID',
  `skip_date` DATE NOT NULL COMMENT '해제한 날짜',
  `created_by` BIGINT NOT NULL COMMENT '해제한 사용자',
  `created_at` DATETIME NOT NULL COMMENT '생성 시각',
  UNIQUE KEY `uk_exception_availability_date` (`availability_id`, `skip_date`),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `unidentified_report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '미확인 기록 ID (해제 후 7일 뒤 삭제)',
  `slot_id` BIGINT NOT NULL COMMENT '주차면 ID',
  `plate_number` VARCHAR(12) NULL COMMENT '차량번호 (모르면 NULL)',
  `reported_by` BIGINT NOT NULL COMMENT '기록한 사용자 (관리자·면 주인)',
  `reported_at` DATETIME NOT NULL COMMENT '기록 시각',
  `resolved_at` DATETIME NULL COMMENT '해제 시각 (NULL이면 미해제)',
  `resolved_by` BIGINT NULL COMMENT '해제한 사용자 (AUTO면 NULL)',
  `resolve_type` VARCHAR(10) NULL COMMENT 'LEFT(차량 떠남) / MINE(내 차예요) / AUTO(정상 입차로 자동 해제)',
  `open_slot_id` BIGINT AS (IF(`resolved_at` IS NULL, `slot_id`, NULL)) STORED COMMENT '미해제면 slot_id (면당 1건 보장용)',
  CONSTRAINT `ck_report_resolve` CHECK (
    (`resolved_at` IS NULL AND `resolved_by` IS NULL AND `resolve_type` IS NULL)
    OR (`resolved_at` IS NOT NULL AND `resolved_at` >= `reported_at` AND `resolve_type` IS NOT NULL AND `resolve_type` IN ('LEFT', 'MINE', 'AUTO'))
  ),
  UNIQUE KEY `uk_report_open_slot` (`open_slot_id`),
  KEY `idx_report_resolved` (`resolved_at`),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `villa` ADD CONSTRAINT `fk_villa_admin_user` FOREIGN KEY (`admin_user_id`) REFERENCES `users` (`id`);
ALTER TABLE `household` ADD CONSTRAINT `fk_household_villa` FOREIGN KEY (`villa_id`) REFERENCES `villa` (`id`);
ALTER TABLE `users` ADD CONSTRAINT `fk_users_household` FOREIGN KEY (`household_id`) REFERENCES `household` (`id`);
ALTER TABLE `parking_slot` ADD CONSTRAINT `fk_slot_villa` FOREIGN KEY (`villa_id`) REFERENCES `villa` (`id`);
ALTER TABLE `parking_slot` ADD CONSTRAINT `fk_slot_household` FOREIGN KEY (`household_id`) REFERENCES `household` (`id`);
ALTER TABLE `vehicle` ADD CONSTRAINT `fk_vehicle_villa` FOREIGN KEY (`villa_id`) REFERENCES `villa` (`id`);
ALTER TABLE `vehicle` ADD CONSTRAINT `fk_vehicle_household` FOREIGN KEY (`household_id`) REFERENCES `household` (`id`);
ALTER TABLE `vehicle` ADD CONSTRAINT `fk_vehicle_contact_user` FOREIGN KEY (`contact_user_id`) REFERENCES `users` (`id`);
ALTER TABLE `parking_record` ADD CONSTRAINT `fk_record_slot` FOREIGN KEY (`slot_id`) REFERENCES `parking_slot` (`id`);
ALTER TABLE `parking_record` ADD CONSTRAINT `fk_record_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicle` (`id`);
ALTER TABLE `parking_record` ADD CONSTRAINT `fk_record_entered_by` FOREIGN KEY (`entered_by`) REFERENCES `users` (`id`);
ALTER TABLE `parking_record` ADD CONSTRAINT `fk_record_exited_by` FOREIGN KEY (`exited_by`) REFERENCES `users` (`id`);
ALTER TABLE `visit` ADD CONSTRAINT `fk_visit_household` FOREIGN KEY (`household_id`) REFERENCES `household` (`id`);
ALTER TABLE `visit` ADD CONSTRAINT `fk_visit_slot` FOREIGN KEY (`slot_id`) REFERENCES `parking_slot` (`id`);
ALTER TABLE `visit` ADD CONSTRAINT `fk_visit_registered_by` FOREIGN KEY (`registered_by`) REFERENCES `users` (`id`);
ALTER TABLE `visit` ADD CONSTRAINT `fk_visit_ended_by` FOREIGN KEY (`ended_by`) REFERENCES `users` (`id`);
ALTER TABLE `frequent_vehicle` ADD CONSTRAINT `fk_frequent_household` FOREIGN KEY (`household_id`) REFERENCES `household` (`id`);
ALTER TABLE `availability` ADD CONSTRAINT `fk_availability_slot` FOREIGN KEY (`slot_id`) REFERENCES `parking_slot` (`id`);
ALTER TABLE `availability` ADD CONSTRAINT `fk_availability_registered_by` FOREIGN KEY (`registered_by`) REFERENCES `users` (`id`);
ALTER TABLE `availability_exception` ADD CONSTRAINT `fk_exception_availability` FOREIGN KEY (`availability_id`) REFERENCES `availability` (`id`) ON DELETE CASCADE;
ALTER TABLE `availability_exception` ADD CONSTRAINT `fk_exception_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`);
ALTER TABLE `unidentified_report` ADD CONSTRAINT `fk_report_slot` FOREIGN KEY (`slot_id`) REFERENCES `parking_slot` (`id`);
ALTER TABLE `unidentified_report` ADD CONSTRAINT `fk_report_reported_by` FOREIGN KEY (`reported_by`) REFERENCES `users` (`id`);
ALTER TABLE `unidentified_report` ADD CONSTRAINT `fk_report_resolved_by` FOREIGN KEY (`resolved_by`) REFERENCES `users` (`id`);

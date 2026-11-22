package com.slotty.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// 스프링 설정 전체가 뜨는지 확인 (MySQL·Redis가 떠 있어야 함: docker compose up -d)
// jwt.secret은 환경 변수로만 받으므로 테스트 전용 값을 직접 넣음 (실제 서버 키와 무관, 공개돼도 괜찮은 값)
@SpringBootTest(properties = "jwt.secret=czTskJ3WaRxM6XvIEtw7wbmR7DGFvWKCLGzcO7zub/4QsPO+YJzce7iPO9jZrUZb")
class SlottyServerApplicationTests {

	@Test
	void contextLoads() {
	}

}

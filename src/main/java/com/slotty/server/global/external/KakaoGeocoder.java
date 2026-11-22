package com.slotty.server.global.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

// 카카오 로컬 API: 주소 → 좌표 (빌라 기준점)
// 실패해도 예외를 던지지 않고 empty → 빌라 생성·수정은 성공, 응답에 geocodeFailed: true (명세)
@Slf4j
@Component
public class KakaoGeocoder {

    private static final String ADDRESS_SEARCH_URL = "https://dapi.kakao.com/v2/local/search/address.json?query={query}";

    private final RestClient restClient;
    private final String apiKey;

    public KakaoGeocoder(@Value("${kakao.rest-api-key:}") String apiKey) {
        this.apiKey = apiKey;
        // 외부 API가 느려도 우리 요청이 오래 묶이지 않게 연결·응답 각각 3초 제한
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public Optional<Coordinates> geocode(String address) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("KAKAO_REST_API_KEY가 없어 주소 변환을 건너뜀");
            return Optional.empty();
        }
        try {
            KakaoAddressResponse response = restClient.get()
                    .uri(ADDRESS_SEARCH_URL, address)           // {query} 자리에 주소 (URL 인코딩 자동)
                    .header("Authorization", "KakaoAK " + apiKey)
                    .retrieve()
                    .body(KakaoAddressResponse.class);
            if (response == null || response.documents() == null || response.documents().isEmpty()) {
                log.warn("주소 변환 결과 없음: {}", address);
                return Optional.empty();
            }
            Document first = response.documents().get(0);   // 가장 정확한 결과
            return Optional.of(new Coordinates(
                    new BigDecimal(first.y()).setScale(7, RoundingMode.HALF_UP),  // y = 위도
                    new BigDecimal(first.x()).setScale(7, RoundingMode.HALF_UP))); // x = 경도
        } catch (Exception e) { // 키 오류(401), 시간 초과, 응답 형식 변경 등
            log.warn("주소 변환 실패: {} ({})", address, e.getMessage());
            return Optional.empty();
        }
    }

    // 카카오 응답에서 필요한 필드만 (나머지 필드는 무시)
    @JsonIgnoreProperties(ignoreUnknown = true)
    record KakaoAddressResponse(List<Document> documents) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Document(String x, String y) {
    }
}

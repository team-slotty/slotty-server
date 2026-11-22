package com.slotty.server.domain.board.sse;

import com.slotty.server.domain.board.event.BoardChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

// 23. 현황판 SSE: 빌라별로 열린 연결(SseEmitter)을 들고 있다가 변경 이벤트를 보냄
// 서버가 1대라 메모리에 보관 (여러 대가 되면 Redis Pub/Sub으로 서버끼리 이벤트를 나눠야 함)
@Slf4j
@Component
public class BoardSseHub {

    private final long timeoutMillis; // 이 시간이 지나면 서버가 닫음 → 앱이 다시 연결 (기본 30분)
    private final Map<Long, Set<SseEmitter>> emittersByVilla = new ConcurrentHashMap<>();

    public BoardSseHub(@Value("${sse.timeout-ms:1800000}") long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public SseEmitter subscribe(Long villaId) {
        SseEmitter emitter = new SseEmitter(timeoutMillis);
        Set<SseEmitter> emitters = emittersByVilla.computeIfAbsent(villaId, id -> new CopyOnWriteArraySet<>());
        emitters.add(emitter);

        // 끝나거나(앱이 닫음) 시간이 다 되거나 오류가 나면 목록에서 뺌 (안 빼면 죽은 연결에 계속 보내며 메모리가 샘)
        Runnable remove = () -> emitters.remove(emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(() -> {
            remove.run();
            emitter.complete();
        });
        emitter.onError(e -> remove.run());

        // 연결 직후 한 번 보냄: 응답 헤더가 바로 나가서 앱이 "연결됨"을 알 수 있고, 프록시가 빈 응답으로 끊지 않음
        send(villaId, emitter, SseEmitter.event().name("connected").data("{}"));
        return emitter;
    }

    // 커밋된 뒤에만 보냄: 롤백된 변경을 알리면 앱이 다시 불러도 바뀐 게 없음
    // 트랜잭션 밖에서 발행된 이벤트도 보내도록 fallbackExecution = true
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBoardChanged(BoardChangedEvent event) {
        Set<SseEmitter> emitters = emittersByVilla.get(event.villaId());
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        String data = "{\"slotIds\":" + event.slotIds() + "}"; // 예: {"slotIds":[2, 5]}
        for (SseEmitter emitter : emitters) {
            send(event.villaId(), emitter, SseEmitter.event().name("board-changed").data(data));
        }
    }

    // 25초마다 주석 한 줄(: ping): 연결이 끊긴 앱을 찾아내고, 중간 장비가 조용한 연결을 끊지 않게 함
    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        emittersByVilla.forEach((villaId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                send(villaId, emitter, SseEmitter.event().comment("ping"));
            }
        });
    }

    // 지금 연결 수 (확인·로그용)
    public int connectionCount() {
        return emittersByVilla.values().stream().mapToInt(Set::size).sum();
    }

    private void send(Long villaId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException e) { // 앱이 이미 끊음 → 목록에서 빼고 정리
            Set<SseEmitter> emitters = emittersByVilla.get(villaId);
            if (emitters != null) {
                emitters.remove(emitter);
            }
            emitter.completeWithError(e);
        }
    }
}

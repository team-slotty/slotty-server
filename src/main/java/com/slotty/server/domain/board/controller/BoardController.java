package com.slotty.server.domain.board.controller;

import com.slotty.server.domain.board.dto.BoardResponse;
import com.slotty.server.domain.board.dto.ParkingBoardResponse;
import com.slotty.server.domain.board.service.BoardService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class BoardController {

    private final BoardService boardService;

    // 초기 현황판 (iOS가 이미 연결해서 그대로 둠. 새 화면은 /api/parking-board로 옮긴 뒤 삭제 예정)
    @GetMapping("/api/board")
    public BoardResponse getBoard(@AuthenticationPrincipal Long userId) {
        return boardService.getBoard(userId);
    }

    // 22. 주차 현황판 (명세 전체: 배치 좌표 + 7가지 상태 + 방문 + 내 차 + 오늘 방문)
    @GetMapping("/api/parking-board")
    public ParkingBoardResponse getParkingBoard(@AuthenticationPrincipal Long userId) {
        return boardService.getParkingBoard(userId);
    }
}

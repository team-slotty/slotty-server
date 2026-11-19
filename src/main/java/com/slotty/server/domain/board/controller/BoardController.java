package com.slotty.server.domain.board.controller;

import com.slotty.server.domain.board.dto.BoardResponse;
import com.slotty.server.domain.board.service.BoardService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class BoardController {
    private final BoardService boardService;

    @GetMapping("/api/board")
    public BoardResponse getBoard(@AuthenticationPrincipal Long userId) {
        return boardService.getBoard(userId);
    }
}

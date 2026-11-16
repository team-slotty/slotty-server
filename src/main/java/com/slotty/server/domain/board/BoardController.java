package com.slotty.server.domain.board;

import com.slotty.server.domain.board.dto.BoardResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BoardController {

    @GetMapping("/api/board")
    public BoardResponse getBoard(){
        return new BoardResponse("Green Villa");
    }
}

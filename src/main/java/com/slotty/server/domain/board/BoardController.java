package com.slotty.server.domain.board;

import com.slotty.server.domain.board.dto.BoardResponse;
import com.slotty.server.domain.board.dto.BoardResponse.Slot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
public class BoardController {

    @GetMapping("/api/board")
    public BoardResponse getBoard() {
        return new BoardResponse(
                "Green Villa",
                List.of(
                        new Slot(1, "101", "OCCUPIED", new BoardResponse.Occupant("3255", today(8, 30))),
                        new Slot(2, "201", "OCCUPIED",     new BoardResponse.Occupant("12가3456", today(19, 12))),
                        new Slot(3, "102", "EMPTY",        null),
                        new Slot(4, "202", "EMPTY",        null),
                        new Slot(5, "302", "VISITING",     new BoardResponse.Occupant("34다1234", today(14, 0))),
                        new Slot(6, "301", "UNIDENTIFIED", new BoardResponse.Occupant(null, today(21, 10))),
                        new Slot(7, null,  "EMPTY",        null)
                )
        );
    }

    private LocalDateTime today(int h, int m) {
        return LocalDateTime.of(LocalDate.now(), LocalTime.of(h, m));
    }
}

package com.slotty.server.domain.user.controller;

import com.slotty.server.domain.user.dto.MeResponse;
import com.slotty.server.domain.user.dto.MeUpdateRequest;
import com.slotty.server.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 5. 내 정보
    @GetMapping("/api/me")
    public MeResponse getMe(@AuthenticationPrincipal Long userId) {
        return userService.getMe(userId);
    }

    // 6. 내 정보 수정 (보낸 필드만)
    @PatchMapping("/api/me")
    public MeResponse updateMe(@AuthenticationPrincipal Long userId, @Valid @RequestBody MeUpdateRequest request) {
        return userService.updateMe(userId, request);
    }

    // 7. 탈퇴
    @DeleteMapping("/api/me")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal Long userId) {
        userService.withdraw(userId);
        return ResponseEntity.noContent().build();
    }
}

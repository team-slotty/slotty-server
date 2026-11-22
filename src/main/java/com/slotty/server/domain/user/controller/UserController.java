package com.slotty.server.domain.user.controller;

import com.slotty.server.domain.user.dto.MeResponse;
import com.slotty.server.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/api/me")
    public MeResponse getMe(@AuthenticationPrincipal Long userId) {
        return userService.getMe(userId);
    }
}

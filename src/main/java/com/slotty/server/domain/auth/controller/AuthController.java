package com.slotty.server.domain.auth.controller;

import com.slotty.server.domain.auth.dto.LoginRequest;
import com.slotty.server.domain.auth.dto.ReissueRequest;
import com.slotty.server.domain.auth.dto.TokenResponse;
import com.slotty.server.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/api/auth/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/api/auth/reissue")
    public TokenResponse reissue(
            @Valid
            @RequestBody
            ReissueRequest reissueRequest
        ) {
        return authService.reissue(reissueRequest);
    }
}
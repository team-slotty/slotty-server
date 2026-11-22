package com.slotty.server.domain.auth.controller;

import com.slotty.server.domain.auth.dto.LoginRequest;
import com.slotty.server.domain.auth.dto.LogoutRequest;
import com.slotty.server.domain.auth.dto.ReissueRequest;
import com.slotty.server.domain.auth.dto.TokenResponse;
import com.slotty.server.domain.auth.service.AuthService;
import com.slotty.server.domain.auth.dto.SignupRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/api/auth/signup")
    public ResponseEntity<TokenResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request)); // 201
    }

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

    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout( // ResponseEntity<Void> == 본문이 없는 응답
            @Valid
            @RequestBody
            LogoutRequest logoutRequest
    ) {
        authService.logout(logoutRequest);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}

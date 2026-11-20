package com.slotty.server.domain.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}
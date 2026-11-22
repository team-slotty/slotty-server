package com.slotty.server.domain.villa.dto;

import jakarta.validation.constraints.Size;

// PATCH: 보낸 필드만 바뀜 (null = 그대로)
public record VillaUpdateRequest(
        @Size(min = 1, max = 50) String name,
        @Size(min = 1, max = 200) String address
) {
}

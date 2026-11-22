package com.slotty.server.domain.villa.dto;

public record VillaUpdateResponse(Long id, String name, String address, AnchorResponse anchor, boolean geocodeFailed) {
}

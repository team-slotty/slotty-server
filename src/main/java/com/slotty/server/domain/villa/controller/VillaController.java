package com.slotty.server.domain.villa.controller;

import com.slotty.server.domain.villa.dto.AdminDelegateRequest;
import com.slotty.server.domain.villa.service.VillaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/villas")
public class VillaController {

    private final VillaService villaService;

    @PutMapping("/me/admin")
    public ResponseEntity<Void> delegateAdmin(@AuthenticationPrincipal Long userId,
                                              @Valid @RequestBody AdminDelegateRequest request) {
        villaService.delegateAdmin(userId, request);
        return ResponseEntity.noContent().build();
    }
}

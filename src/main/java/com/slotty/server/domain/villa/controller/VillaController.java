package com.slotty.server.domain.villa.controller;

import com.slotty.server.domain.villa.dto.*;
import com.slotty.server.domain.villa.service.VillaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/villas")
public class VillaController {

    private final VillaService villaService;

    // 8. 빌라 온보딩 (로그인·미소속)
    @PostMapping
    public ResponseEntity<VillaCreateResponse> createVilla(@AuthenticationPrincipal Long userId,
                                                           @Valid @RequestBody VillaCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(villaService.createVilla(userId, request));
    }

    // 9. 빌라 정보 (입주민)
    @GetMapping("/me")
    public VillaResponse getMyVilla(@AuthenticationPrincipal Long userId) {
        return villaService.getMyVilla(userId);
    }

    // 10. 이름·주소 수정 (관리자)
    @PatchMapping("/me")
    public VillaUpdateResponse updateVilla(@AuthenticationPrincipal Long userId,
                                           @Valid @RequestBody VillaUpdateRequest request) {
        return villaService.updateVilla(userId, request);
    }

    // 11. 기준점 재설정·현장 보정 (관리자)
    @PutMapping("/me/anchor")
    public AnchorUpdateResponse updateAnchor(@AuthenticationPrincipal Long userId,
                                             @Valid @RequestBody AnchorRequest request) {
        return villaService.updateAnchor(userId, request);
    }

    // 12. 배치 변경 (관리자)
    @PutMapping("/me/layout")
    public LayoutResponse changeLayout(@AuthenticationPrincipal Long userId,
                                       @Valid @RequestBody LayoutRequest request) {
        return villaService.changeLayout(userId, request);
    }

    // 13. 관리자 위임 (관리자)
    @PutMapping("/me/admin")
    public ResponseEntity<Void> delegateAdmin(@AuthenticationPrincipal Long userId,
                                              @Valid @RequestBody AdminDelegateRequest request) {
        villaService.delegateAdmin(userId, request);
        return ResponseEntity.noContent().build();
    }
}

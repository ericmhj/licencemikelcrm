package com.mikelcrm.licenseservice.controller;

import com.mikelcrm.licenseservice.security.TenantAuthenticationToken;
import com.mikelcrm.licenseservice.service.query.AccessQueryService;
import com.mikelcrm.licenseservice.service.query.dto.SuspendedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AccessController {

    private final AccessQueryService accessQueryService;

    @GetMapping("/access/{tenantId}")
    public ResponseEntity<?> validateAccess(@PathVariable UUID tenantId, Authentication auth) {
        TenantAuthenticationToken tenantAuth = (TenantAuthenticationToken) auth;
        UUID userId = tenantAuth.getUserId();

        Object response = accessQueryService.getAccess(tenantId, userId);

        if (response instanceof SuspendedResponse) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        return ResponseEntity.ok(response);
    }
}

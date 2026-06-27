package com.mikelcrm.licenseservice.controller;

import com.mikelcrm.licenseservice.service.notification.EmailService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final EmailService emailService;

    @PostMapping("/welcome")
    public ResponseEntity<Map<String, String>> sendWelcomeEmail(@Valid @RequestBody WelcomeEmailRequest request) {
        emailService.sendWelcomeEmail(
            request.getEmail(),
            request.getNombre(),
            request.getTenantId(),
            request.getModalidad(),
            request.getCreditosBienvenida(),
            request.getModulos(),
            request.getCuotaMensual()
        );
        return ResponseEntity.ok(Map.of("status", "sent", "message", "Email de bienvenida enviado"));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WelcomeEmailRequest {
        @NotBlank private String email;
        @NotBlank private String nombre;
        @NotBlank private String tenantId;
        @NotBlank private String modalidad;
        @NotNull private Integer creditosBienvenida;
        @NotNull private List<String> modulos;
        @NotNull private BigDecimal cuotaMensual;
    }
}

package com.mikelcrm.licenseservice.controller;

import com.mikelcrm.licenseservice.payment.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentGatewayFactory gatewayFactory;

    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody PaymentRequest request) {
        PaymentGateway gateway = gatewayFactory.getActiveGateway();
        PaymentResponse response = gateway.processPayment(request);
        return ResponseEntity.ok(response);
    }
}

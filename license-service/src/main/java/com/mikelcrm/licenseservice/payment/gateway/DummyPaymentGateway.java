package com.mikelcrm.licenseservice.payment.gateway;

import com.mikelcrm.licenseservice.payment.*;
import com.mikelcrm.licenseservice.payment.validation.CardValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
@Slf4j
public class DummyPaymentGateway implements PaymentGateway {

    @Override
    public String getGatewayId() { return "DUMMY"; }

    @Override
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("DummyGateway processing payment: amount={}, card=****{}",
            request.getAmount(), request.getCardNumber().substring(request.getCardNumber().length() - 4));

        // Validate card format
        if (!CardValidator.isValidCardNumber(request.getCardNumber())) {
            return PaymentResponse.builder()
                .status("REJECTED").transactionId(UUID.randomUUID())
                .message("Número de tarjeta inválido").gatewayId(getGatewayId()).build();
        }
        if (!CardValidator.isValidExpiry(request.getExpiryDate())) {
            return PaymentResponse.builder()
                .status("REJECTED").transactionId(UUID.randomUUID())
                .message("Fecha de expiración inválida o vencida").gatewayId(getGatewayId()).build();
        }
        if (!CardValidator.isValidCvv(request.getCvv())) {
            return PaymentResponse.builder()
                .status("REJECTED").transactionId(UUID.randomUUID())
                .message("CVV inválido").gatewayId(getGatewayId()).build();
        }

        // Dummy: always approve if validations pass
        return PaymentResponse.builder()
            .status("APPROVED").transactionId(UUID.randomUUID())
            .message("Pago aprobado (entorno de desarrollo)").gatewayId(getGatewayId()).build();
    }
}

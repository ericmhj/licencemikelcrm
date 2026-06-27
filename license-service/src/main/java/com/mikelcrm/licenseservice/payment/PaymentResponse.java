package com.mikelcrm.licenseservice.payment;

import lombok.*;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentResponse {
    private String status;          // APPROVED, REJECTED, ERROR
    private UUID transactionId;
    private String message;
    private String gatewayId;       // Which gateway processed it
}

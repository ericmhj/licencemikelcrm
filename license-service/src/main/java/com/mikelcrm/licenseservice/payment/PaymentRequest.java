package com.mikelcrm.licenseservice.payment;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentRequest {
    @NotBlank private String cardNumber;      // 16 digits
    @NotBlank private String holderName;
    @NotBlank private String expiryDate;      // MM/YY
    @NotBlank private String cvv;             // 3-4 digits
    @NotNull @DecimalMin("0.01") private BigDecimal amount;
    private String currency;                   // default EUR
    private String description;
    private String paymentMethodId;            // Stripe.js payment method (optional)
}

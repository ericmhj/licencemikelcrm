package com.mikelcrm.licenseservice.payment;

/**
 * Strategy interface for payment gateways.
 * To add a new gateway in the future, simply implement this interface
 * and annotate with @Component. No existing code needs modification.
 */
public interface PaymentGateway {
    String getGatewayId();
    PaymentResponse processPayment(PaymentRequest request);
}

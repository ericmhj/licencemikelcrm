package com.mikelcrm.licenseservice.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "license-service.payment")
public class PaymentProperties {
    private String activeGateway = "DUMMY";

    public String getActiveGateway() { return activeGateway; }
    public void setActiveGateway(String activeGateway) { this.activeGateway = activeGateway; }
}

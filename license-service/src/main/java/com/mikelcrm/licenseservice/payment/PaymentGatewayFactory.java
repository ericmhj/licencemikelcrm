package com.mikelcrm.licenseservice.payment;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentGatewayFactory {
    private final Map<String, PaymentGateway> gateways;
    private final PaymentProperties properties;

    public PaymentGatewayFactory(List<PaymentGateway> gatewayList, PaymentProperties properties) {
        this.properties = properties;
        this.gateways = gatewayList.stream()
            .collect(Collectors.toMap(PaymentGateway::getGatewayId, Function.identity()));
    }

    public PaymentGateway getActiveGateway() {
        PaymentGateway gateway = gateways.get(properties.getActiveGateway());
        if (gateway == null) {
            throw new IllegalStateException("No payment gateway found for: " + properties.getActiveGateway());
        }
        return gateway;
    }
}

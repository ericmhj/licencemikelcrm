package com.mikelcrm.licenseservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "license-service.jwt")
public class JwtProperties {

    private String publicKey;
    private long maxTtlMinutes = 15;
    private String issuer = "auth-service";
    private boolean skipValidation = false;
    private boolean trustGatewayHeaders = false;
    private String keycloakJwksUrl;

    public String getKeycloakJwksUrl() {
        return keycloakJwksUrl;
    }

    public void setKeycloakJwksUrl(String keycloakJwksUrl) {
        this.keycloakJwksUrl = keycloakJwksUrl;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public long getMaxTtlMinutes() {
        return maxTtlMinutes;
    }

    public void setMaxTtlMinutes(long maxTtlMinutes) {
        this.maxTtlMinutes = maxTtlMinutes;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public boolean isSkipValidation() {
        return skipValidation;
    }

    public void setSkipValidation(boolean skipValidation) {
        this.skipValidation = skipValidation;
    }

    public boolean isTrustGatewayHeaders() {
        return trustGatewayHeaders;
    }

    public void setTrustGatewayHeaders(boolean trustGatewayHeaders) {
        this.trustGatewayHeaders = trustGatewayHeaders;
    }
}

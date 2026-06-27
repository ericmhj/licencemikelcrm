package com.mikelcrm.licenseservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for per-Tenant rate limiting using Redis sliding window counters.
 */
@ConfigurationProperties(prefix = "license-service.rate-limit")
public class RateLimitProperties {

    /**
     * Maximum requests per second allowed for Query API (GET) per Tenant.
     */
    private int queryMaxRequestsPerSecond = 1000;

    /**
     * Maximum requests per second allowed for Command API (POST/PUT/DELETE/PATCH) per Tenant.
     */
    private int commandMaxRequestsPerSecond = 100;

    /**
     * Window size in seconds for the sliding window counter.
     */
    private int windowSizeSeconds = 1;

    public int getQueryMaxRequestsPerSecond() {
        return queryMaxRequestsPerSecond;
    }

    public void setQueryMaxRequestsPerSecond(int queryMaxRequestsPerSecond) {
        this.queryMaxRequestsPerSecond = queryMaxRequestsPerSecond;
    }

    public int getCommandMaxRequestsPerSecond() {
        return commandMaxRequestsPerSecond;
    }

    public void setCommandMaxRequestsPerSecond(int commandMaxRequestsPerSecond) {
        this.commandMaxRequestsPerSecond = commandMaxRequestsPerSecond;
    }

    public int getWindowSizeSeconds() {
        return windowSizeSeconds;
    }

    public void setWindowSizeSeconds(int windowSizeSeconds) {
        this.windowSizeSeconds = windowSizeSeconds;
    }
}

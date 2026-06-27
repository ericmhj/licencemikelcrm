package com.mikelcrm.licenseservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables asynchronous method execution for the application.
 * Used by the AuditAspect to write audit logs without blocking responses.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}

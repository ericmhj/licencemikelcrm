package com.mikelcrm.licenseservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI licenseServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("License Service API — Mikel CRM")
                        .description("Microservicio de gestión de licencias, contratos anuales, créditos, "
                                + "contadores de consultas y control de acceso (RBAC) para la plataforma Mikel CRM.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipo License Service")
                                .email("licencias@mikelcrm.com")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer JWT"))
                .schemaRequirement("Bearer JWT", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT firmado por auth-service con claims: tenantId, userId, rol"));
    }
}

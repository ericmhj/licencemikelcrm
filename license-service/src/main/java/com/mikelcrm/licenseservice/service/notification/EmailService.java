package com.mikelcrm.licenseservice.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendWelcomeEmail(String toEmail, String tenantName, String tenantId,
                                  String modalidad, int creditosBienvenida,
                                  List<String> modulos, BigDecimal cuotaMensual) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("admin@normalia.com.mx");
            message.setTo(toEmail);
            message.setSubject("🎉 ¡Bienvenido a Mikel CRM! — Confirmación de contrato");

            StringBuilder body = new StringBuilder();
            body.append("═══════════════════════════════════════════════\n");
            body.append("   ¡FELICIDADES POR TU NUEVO SERVICIO!\n");
            body.append("═══════════════════════════════════════════════\n\n");
            body.append("Hola ").append(tenantName).append(",\n\n");
            body.append("Tu contrato con Mikel CRM ha sido activado exitosamente.\n\n");
            body.append("───────────────────────────────────────────────\n");
            body.append("   DETALLES DEL CONTRATO\n");
            body.append("───────────────────────────────────────────────\n");
            body.append("ID del Tenant: ").append(tenantId).append("\n");
            body.append("Modalidad: ").append(modalidad).append("\n");
            body.append("Créditos de bienvenida: ").append(creditosBienvenida).append("\n\n");
            body.append("───────────────────────────────────────────────\n");
            body.append("   MÓDULOS CONTRATADOS\n");
            body.append("───────────────────────────────────────────────\n");
            for (String modulo : modulos) {
                body.append("  ✓ ").append(modulo).append("\n");
            }
            body.append("\n");
            body.append("Cuota mensual: ").append(cuotaMensual).append(" €\n\n");
            body.append("───────────────────────────────────────────────\n");
            body.append("   PRÓXIMOS PASOS\n");
            body.append("───────────────────────────────────────────────\n");
            body.append("1. Tu CRM ya está activo\n");
            body.append("2. Accede desde: https://app.mikelcrm.com\n");
            body.append("3. Primer cobro de cuota: en 30 días\n\n");
            body.append("───────────────────────────────────────────────\n");
            body.append("   POLÍTICA DE DESCUENTOS\n");
            body.append("───────────────────────────────────────────────\n");
            body.append("• 10% descuento: pago antes de la fecha límite\n");
            body.append("• 3% descuento: pago en la fecha límite\n");
            body.append("• Sin descuento: pago posterior\n\n");
            body.append("Gracias por confiar en Mikel CRM.\n");
            body.append("Equipo Mikel CRM\n");

            message.setText(body.toString());
            mailSender.send(message);

            log.info("Welcome email sent to {} for tenant {}", toEmail, tenantId);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
        }
    }
}

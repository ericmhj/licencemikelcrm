package com.mikelcrm.licenseservice.service.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mikelcrm.licenseservice.domain.entity.ContratoAnual;
import com.mikelcrm.licenseservice.domain.entity.PaqueteCreditos;
import com.mikelcrm.licenseservice.domain.entity.RolUsuario;
import com.mikelcrm.licenseservice.domain.entity.Tenant;
import com.mikelcrm.licenseservice.domain.enums.EstadoContrato;
import com.mikelcrm.licenseservice.domain.enums.EstadoPaquete;
import com.mikelcrm.licenseservice.domain.enums.EstadoTenant;
import com.mikelcrm.licenseservice.domain.repository.ContratoAnualRepository;
import com.mikelcrm.licenseservice.domain.repository.CuotaAlmacenamientoRepository;
import com.mikelcrm.licenseservice.domain.repository.CuotaMensualRepository;
import com.mikelcrm.licenseservice.domain.repository.PaqueteCreditosRepository;
import com.mikelcrm.licenseservice.domain.repository.RolUsuarioRepository;
import com.mikelcrm.licenseservice.domain.repository.TenantRepository;
import com.mikelcrm.licenseservice.domain.enums.EstadoCuota;
import com.mikelcrm.licenseservice.domain.enums.EstadoCuotaAlmacenamiento;
import com.mikelcrm.licenseservice.exception.TenantNotFoundException;
import com.mikelcrm.licenseservice.service.query.dto.AccessResponse;
import com.mikelcrm.licenseservice.service.query.dto.SuspendedResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccessQueryService {

    private final StringRedisTemplate redisTemplate;
    private final TenantRepository tenantRepository;
    private final ContratoAnualRepository contratoRepository;
    private final RolUsuarioRepository rolUsuarioRepository;
    private final PaqueteCreditosRepository paqueteRepository;
    private final CuotaMensualRepository cuotaMensualRepository;
    private final CuotaAlmacenamientoRepository cuotaAlmacenamientoRepository;
    private final ObjectMapper objectMapper;

    private static final String CACHE_KEY_PREFIX = "tenant:";
    private static final String CACHE_KEY_SUFFIX = ":access";
    private static final long CACHE_TTL_SECONDS = 300;

    /**
     * Validates tenant access. Tries Redis cache first, falls back to PostgreSQL on miss.
     * Returns AccessResponse for active tenants, SuspendedResponse for suspended tenants.
     */
    public Object getAccess(UUID tenantId, UUID userId) {
        String cacheKey = CACHE_KEY_PREFIX + tenantId + CACHE_KEY_SUFFIX;

        // 1. Try Redis cache first
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return deserializeCachedResponse(cached);
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize cached access for tenant {}, rebuilding from DB", tenantId, e);
            }
        }

        // 2. Cache miss → query PostgreSQL
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        if (tenant.getEstado() == EstadoTenant.SUSPENDED) {
            SuspendedResponse response = buildSuspendedResponse(tenant);
            cacheResponse(cacheKey, response);
            return response;
        }

        AccessResponse response = buildFromDatabase(tenantId, userId, tenant);
        cacheResponse(cacheKey, response);
        return response;
    }

    private AccessResponse buildFromDatabase(UUID tenantId, UUID userId, Tenant tenant) {
        // Get active modules
        List<ContratoAnual> activeContracts = contratoRepository.findByTenantIdAndEstado(
                tenantId, EstadoContrato.ACTIVE);
        List<String> modules = activeContracts.stream()
                .filter(c -> c.getModulo() != null)
                .map(ContratoAnual::getModulo)
                .collect(Collectors.toList());

        // Get credit balance
        BigDecimal creditBalance = paqueteRepository
                .findByTenantIdAndEstado(tenantId, EstadoPaquete.ACTIVE)
                .map(PaqueteCreditos::getSaldoDisponible)
                .orElse(BigDecimal.ZERO);

        // Get user role
        String userRole = rolUsuarioRepository.findByUsuarioIdAndTenantId(userId, tenantId)
                .map(rol -> rol.getRol().name())
                .orElse(null);

        return AccessResponse.builder()
                .tenantId(tenantId)
                .status(tenant.getEstado().name())
                .modules(modules)
                .creditBalance(creditBalance)
                .userRole(userRole)
                .cachedAt(Instant.now().toString())
                .build();
    }

    private SuspendedResponse buildSuspendedResponse(Tenant tenant) {
        // Calculate total debt: overdue cuotas + storage fees
        BigDecimal cuotasVencidas = cuotaMensualRepository
                .findByTenantIdAndEstadoIn(tenant.getId(), List.of(EstadoCuota.VENCIDA, EstadoCuota.EN_MORA))
                .stream()
                .map(c -> c.getMontoOriginal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal almacenamiento = cuotaAlmacenamientoRepository
                .findByTenantIdAndEstado(tenant.getId(), EstadoCuotaAlmacenamiento.PENDIENTE)
                .stream()
                .map(c -> c.getMonto())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal adeudoTotal = cuotasVencidas.add(almacenamiento);

        String suspendedSince = tenant.getFechaSuspension() != null
                ? tenant.getFechaSuspension().toLocalDate().format(DateTimeFormatter.ISO_DATE)
                : null;

        return SuspendedResponse.builder()
                .tenantId(tenant.getId())
                .status("SUSPENDED")
                .suspendedSince(suspendedSince)
                .adeudoTotal(adeudoTotal)
                .reactivationUrl("/api/v1/tenants/" + tenant.getId() + "/reactivation-summary")
                .build();
    }

    private Object deserializeCachedResponse(String json) throws JsonProcessingException {
        // Try AccessResponse first, then SuspendedResponse
        if (json.contains("\"status\":\"SUSPENDED\"")) {
            return objectMapper.readValue(json, SuspendedResponse.class);
        }
        return objectMapper.readValue(json, AccessResponse.class);
    }

    private void cacheResponse(String cacheKey, Object response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize access response for cache", e);
        }
    }

}

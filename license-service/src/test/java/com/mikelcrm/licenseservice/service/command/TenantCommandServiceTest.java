package com.mikelcrm.licenseservice.service.command;

import com.mikelcrm.licenseservice.domain.entity.CuotaAlmacenamiento;
import com.mikelcrm.licenseservice.domain.entity.CuotaMensual;
import com.mikelcrm.licenseservice.domain.entity.Tenant;
import com.mikelcrm.licenseservice.domain.enums.EstadoCuota;
import com.mikelcrm.licenseservice.domain.enums.EstadoCuotaAlmacenamiento;
import com.mikelcrm.licenseservice.domain.enums.EstadoTenant;
import com.mikelcrm.licenseservice.domain.enums.ModalidadReporte;
import com.mikelcrm.licenseservice.domain.repository.CuotaAlmacenamientoRepository;
import com.mikelcrm.licenseservice.domain.repository.CuotaMensualRepository;
import com.mikelcrm.licenseservice.domain.repository.TenantRepository;
import com.mikelcrm.licenseservice.exception.InvalidStateTransitionException;
import com.mikelcrm.licenseservice.exception.MontoIncorrectoException;
import com.mikelcrm.licenseservice.exception.TenantNotFoundException;
import com.mikelcrm.licenseservice.service.cache.CacheInvalidationService;
import com.mikelcrm.licenseservice.event.DomainEventPublisher;
import com.mikelcrm.licenseservice.service.command.dto.CommandResponse;
import com.mikelcrm.licenseservice.service.command.dto.CreateTenantRequest;
import com.mikelcrm.licenseservice.service.command.dto.ReactivateTenantRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantCommandServiceTest {

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private CuotaMensualRepository cuotaMensualRepository;
    @Mock
    private CuotaAlmacenamientoRepository cuotaAlmacenamientoRepository;
    @Mock
    private CacheInvalidationService cacheInvalidationService;
    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private TenantCommandService service;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
    }

    @Test
    void createTenant_shouldCreateInOnboardingState() {
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(i -> i.getArgument(0));

        CreateTenantRequest request = new CreateTenantRequest("Empresa S.A.", "admin@empresa.com", ModalidadReporte.ESTANDAR);
        CommandResponse response = service.createTenant(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getCorrelationId()).isNotNull();

        verify(tenantRepository).save(argThat(t ->
                t.getEstado() == EstadoTenant.ONBOARDING &&
                t.getNombre().equals("Empresa S.A.") &&
                t.getEmailContacto().equals("admin@empresa.com") &&
                t.getModalidadReporte() == ModalidadReporte.ESTANDAR &&
                t.getReportesAlmacenados() == 0 &&
                t.getDeudaAlmacenamiento().compareTo(BigDecimal.ZERO) == 0
        ));
    }

    @Test
    void activateTenant_fromOnboarding_shouldSucceed() {
        Tenant tenant = buildTenant(EstadoTenant.ONBOARDING);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(i -> i.getArgument(0));

        CommandResponse response = service.activateTenant(tenantId);

        assertThat(response.getId()).isEqualTo(tenantId);
        assertThat(tenant.getEstado()).isEqualTo(EstadoTenant.ACTIVE);
        verify(cacheInvalidationService).invalidateAccessCache(tenantId);
    }

    @Test
    void activateTenant_fromActive_shouldThrow() {
        Tenant tenant = buildTenant(EstadoTenant.ACTIVE);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> service.activateTenant(tenantId))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void activateTenant_notFound_shouldThrow() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activateTenant(tenantId))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void suspendTenant_fromActive_shouldSucceed() {
        Tenant tenant = buildTenant(EstadoTenant.ACTIVE);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(i -> i.getArgument(0));

        CommandResponse response = service.suspendTenant(tenantId);

        assertThat(response.getId()).isEqualTo(tenantId);
        assertThat(tenant.getEstado()).isEqualTo(EstadoTenant.SUSPENDED);
        assertThat(tenant.getFechaSuspension()).isNotNull();
        verify(cacheInvalidationService).invalidateAccessCache(tenantId);
    }

    @Test
    void suspendTenant_fromOnboarding_shouldThrow() {
        Tenant tenant = buildTenant(EstadoTenant.ONBOARDING);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> service.suspendTenant(tenantId))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void cancelTenant_fromActive_shouldSucceed() {
        Tenant tenant = buildTenant(EstadoTenant.ACTIVE);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(i -> i.getArgument(0));

        CommandResponse response = service.cancelTenant(tenantId);

        assertThat(response.getId()).isEqualTo(tenantId);
        assertThat(tenant.getEstado()).isEqualTo(EstadoTenant.CANCELLED);
        assertThat(tenant.getFechaCancelacion()).isNotNull();
        verify(cacheInvalidationService).invalidateAccessCache(tenantId);
    }

    @Test
    void cancelTenant_fromSuspended_shouldSucceed() {
        Tenant tenant = buildTenant(EstadoTenant.SUSPENDED);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(i -> i.getArgument(0));

        CommandResponse response = service.cancelTenant(tenantId);

        assertThat(tenant.getEstado()).isEqualTo(EstadoTenant.CANCELLED);
    }

    @Test
    void cancelTenant_fromOnboarding_shouldThrow() {
        Tenant tenant = buildTenant(EstadoTenant.ONBOARDING);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> service.cancelTenant(tenantId))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void reactivateTenant_withCorrectMonto_shouldSucceed() {
        Tenant tenant = buildTenant(EstadoTenant.SUSPENDED);
        tenant.setFechaSuspension(LocalDateTime.now().minusDays(10));
        tenant.setDeudaAlmacenamiento(new BigDecimal("50.00"));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        // Overdue cuotas: 200.00
        CuotaMensual overdueCuota = CuotaMensual.builder()
                .id(UUID.randomUUID())
                .montoOriginal(new BigDecimal("200.00"))
                .estado(EstadoCuota.VENCIDA)
                .descuentoPct(BigDecimal.ZERO)
                .intentoCobro(0)
                .build();
        when(cuotaMensualRepository.findByTenantIdAndEstadoIn(eq(tenantId), anyList()))
                .thenReturn(List.of(overdueCuota));

        // Storage fees: 70.00
        CuotaAlmacenamiento storageFee = CuotaAlmacenamiento.builder()
                .id(UUID.randomUUID())
                .monto(new BigDecimal("70.00"))
                .estado(EstadoCuotaAlmacenamiento.PENDIENTE)
                .build();
        when(cuotaAlmacenamientoRepository.findByTenantIdAndEstado(tenantId, EstadoCuotaAlmacenamiento.PENDIENTE))
                .thenReturn(List.of(storageFee));

        when(tenantRepository.save(any(Tenant.class))).thenAnswer(i -> i.getArgument(0));
        when(cuotaMensualRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(cuotaAlmacenamientoRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        // Total adeudo: 200 + 70 = 270
        ReactivateTenantRequest request = new ReactivateTenantRequest("STRIPE", new BigDecimal("270.00"));
        CommandResponse response = service.reactivateTenant(tenantId, request);

        assertThat(response.getId()).isEqualTo(tenantId);
        assertThat(tenant.getEstado()).isEqualTo(EstadoTenant.ACTIVE);
        assertThat(tenant.getFechaSuspension()).isNull();
        assertThat(tenant.getDeudaAlmacenamiento()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(overdueCuota.getEstado()).isEqualTo(EstadoCuota.PAGADA);
        assertThat(storageFee.getEstado()).isEqualTo(EstadoCuotaAlmacenamiento.PAGADA);
        verify(cacheInvalidationService).invalidateAccessCache(tenantId);
    }

    @Test
    void reactivateTenant_withIncorrectMonto_shouldThrow() {
        Tenant tenant = buildTenant(EstadoTenant.SUSPENDED);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        CuotaMensual overdueCuota = CuotaMensual.builder()
                .id(UUID.randomUUID())
                .montoOriginal(new BigDecimal("200.00"))
                .estado(EstadoCuota.VENCIDA)
                .descuentoPct(BigDecimal.ZERO)
                .intentoCobro(0)
                .build();
        when(cuotaMensualRepository.findByTenantIdAndEstadoIn(eq(tenantId), anyList()))
                .thenReturn(List.of(overdueCuota));
        when(cuotaAlmacenamientoRepository.findByTenantIdAndEstado(tenantId, EstadoCuotaAlmacenamiento.PENDIENTE))
                .thenReturn(List.of());

        // Expected monto: 200, offering 100
        ReactivateTenantRequest request = new ReactivateTenantRequest("STRIPE", new BigDecimal("100.00"));
        assertThatThrownBy(() -> service.reactivateTenant(tenantId, request))
                .isInstanceOf(MontoIncorrectoException.class);
    }

    private Tenant buildTenant(EstadoTenant estado) {
        return Tenant.builder()
                .id(tenantId)
                .nombre("Test Tenant")
                .emailContacto("test@test.com")
                .estado(estado)
                .modalidadReporte(ModalidadReporte.ESTANDAR)
                .fechaAlta(LocalDateTime.now())
                .reportesAlmacenados(0)
                .deudaAlmacenamiento(BigDecimal.ZERO)
                .build();
    }
}

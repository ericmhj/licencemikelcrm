package com.mikelcrm.licenseservice.service.command;

import com.mikelcrm.licenseservice.domain.entity.ContratoAnual;
import com.mikelcrm.licenseservice.domain.entity.CuotaMensual;
import com.mikelcrm.licenseservice.domain.entity.Tenant;
import com.mikelcrm.licenseservice.domain.enums.*;
import com.mikelcrm.licenseservice.domain.repository.ContratoAnualRepository;
import com.mikelcrm.licenseservice.domain.repository.CuotaMensualRepository;
import com.mikelcrm.licenseservice.domain.repository.TenantRepository;
import com.mikelcrm.licenseservice.exception.CuotaVencidaException;
import com.mikelcrm.licenseservice.exception.InvalidStateTransitionException;
import com.mikelcrm.licenseservice.service.cache.CacheInvalidationService;
import com.mikelcrm.licenseservice.event.DomainEventPublisher;
import com.mikelcrm.licenseservice.service.command.dto.CommandResponse;
import com.mikelcrm.licenseservice.service.command.dto.PayCuotaRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CuotaPaymentServiceTest {

    @Mock
    private CuotaMensualRepository cuotaMensualRepository;
    @Mock
    private ContratoAnualRepository contratoRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private CacheInvalidationService cacheInvalidationService;
    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private CuotaPaymentService service;

    private UUID tenantId;
    private UUID contratoId;
    private UUID cuotaId;
    private Tenant tenant;
    private ContratoAnual contrato;
    private CuotaMensual cuota;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        contratoId = UUID.randomUUID();
        cuotaId = UUID.randomUUID();

        tenant = Tenant.builder()
                .id(tenantId)
                .nombre("Test")
                .emailContacto("test@test.com")
                .estado(EstadoTenant.ACTIVE)
                .modalidadReporte(ModalidadReporte.ESTANDAR)
                .reportesAlmacenados(0)
                .deudaAlmacenamiento(BigDecimal.ZERO)
                .build();

        contrato = ContratoAnual.builder()
                .id(contratoId)
                .tenant(tenant)
                .tipo(TipoContrato.MODULO)
                .modulo("ADMIN")
                .estado(EstadoContrato.ACTIVE)
                .cuotaMensual(new BigDecimal("200.00"))
                .cuotasPagadas(0)
                .cuotasTotales(12)
                .fechaInicio(LocalDate.of(2026, 1, 1))
                .fechaVencimiento(LocalDate.of(2027, 1, 1))
                .fechaAniversario(5)
                .renovacionAuto(true)
                .build();

        cuota = CuotaMensual.builder()
                .id(cuotaId)
                .contrato(contrato)
                .numero(2)
                .fechaLimite(LocalDate.of(2026, 7, 5))
                .montoOriginal(new BigDecimal("200.00"))
                .descuentoPct(BigDecimal.ZERO)
                .estado(EstadoCuota.PENDIENTE)
                .intentoCobro(0)
                .build();
    }

    @Test
    void payCuota_beforeDeadline_should10PercentDiscount() {
        setupMocks();
        PayCuotaRequest request = new PayCuotaRequest(LocalDate.of(2026, 7, 3), "STRIPE", null);

        CommandResponse response = service.payCuota(tenantId, contratoId, cuotaId, request);

        assertThat(response.getId()).isEqualTo(cuotaId);
        assertThat(cuota.getEstado()).isEqualTo(EstadoCuota.PAGADA);
        assertThat(cuota.getMontoCobrado()).isEqualByComparingTo(new BigDecimal("180.00"));
        assertThat(cuota.getDescuentoPct()).isEqualByComparingTo(new BigDecimal("10.00"));
        verify(cacheInvalidationService).invalidateAccessCache(tenantId);
    }

    @Test
    void payCuota_onDeadline_should3PercentDiscount() {
        setupMocks();
        PayCuotaRequest request = new PayCuotaRequest(LocalDate.of(2026, 7, 5), "STRIPE", null);

        service.payCuota(tenantId, contratoId, cuotaId, request);

        assertThat(cuota.getMontoCobrado()).isEqualByComparingTo(new BigDecimal("194.00"));
        assertThat(cuota.getDescuentoPct()).isEqualByComparingTo(new BigDecimal("3.00"));
    }

    @Test
    void payCuota_graceDay_noDiscount() {
        setupMocks();
        PayCuotaRequest request = new PayCuotaRequest(LocalDate.of(2026, 7, 6), "STRIPE", null);

        service.payCuota(tenantId, contratoId, cuotaId, request);

        assertThat(cuota.getMontoCobrado()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(cuota.getDescuentoPct()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void payCuota_afterGraceDay_shouldThrowCuotaVencida() {
        setupMocks();
        PayCuotaRequest request = new PayCuotaRequest(LocalDate.of(2026, 7, 7), "STRIPE", null);

        assertThatThrownBy(() -> service.payCuota(tenantId, contratoId, cuotaId, request))
                .isInstanceOf(CuotaVencidaException.class);
    }

    @Test
    void payCuota_firstCuota_createdContract_transitionsToActive() {
        cuota.setNumero(1);
        contrato.setEstado(EstadoContrato.CREATED);
        setupMocks();

        PayCuotaRequest request = new PayCuotaRequest(LocalDate.of(2026, 7, 3), "STRIPE", null);
        service.payCuota(tenantId, contratoId, cuotaId, request);

        assertThat(contrato.getEstado()).isEqualTo(EstadoContrato.ACTIVE);
        assertThat(contrato.getCuotasPagadas()).isEqualTo(1);
    }

    @Test
    void payCuota_alreadyPaid_shouldThrow() {
        cuota.setEstado(EstadoCuota.PAGADA);
        setupMocks();
        PayCuotaRequest request = new PayCuotaRequest(LocalDate.of(2026, 7, 3), "STRIPE", null);

        assertThatThrownBy(() -> service.payCuota(tenantId, contratoId, cuotaId, request))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    private void setupMocks() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(contratoRepository.findById(contratoId)).thenReturn(Optional.of(contrato));
        when(cuotaMensualRepository.findById(cuotaId)).thenReturn(Optional.of(cuota));
        lenient().when(cuotaMensualRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(contratoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }
}

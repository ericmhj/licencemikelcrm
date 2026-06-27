package com.mikelcrm.licenseservice.service.command;

import com.mikelcrm.licenseservice.domain.entity.ContratoAnual;
import com.mikelcrm.licenseservice.domain.entity.CuotaMensual;
import com.mikelcrm.licenseservice.domain.entity.Tenant;
import com.mikelcrm.licenseservice.domain.enums.*;
import com.mikelcrm.licenseservice.domain.repository.ContratoAnualRepository;
import com.mikelcrm.licenseservice.domain.repository.CuotaMensualRepository;
import com.mikelcrm.licenseservice.domain.repository.TenantRepository;
import com.mikelcrm.licenseservice.exception.*;
import com.mikelcrm.licenseservice.service.cache.CacheInvalidationService;
import com.mikelcrm.licenseservice.event.DomainEventPublisher;
import com.mikelcrm.licenseservice.service.command.dto.CommandResponse;
import com.mikelcrm.licenseservice.service.command.dto.CreateContractRequest;
import com.mikelcrm.licenseservice.service.command.dto.RenewContractRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractCommandServiceTest {

    @Mock
    private ContratoAnualRepository contratoRepository;
    @Mock
    private CuotaMensualRepository cuotaMensualRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private CacheInvalidationService cacheInvalidationService;
    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private ContractCommandService service;

    private UUID tenantId;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        tenant = Tenant.builder()
                .id(tenantId)
                .nombre("Test")
                .emailContacto("test@test.com")
                .estado(EstadoTenant.ACTIVE)
                .modalidadReporte(ModalidadReporte.ESTANDAR)
                .reportesAlmacenados(0)
                .deudaAlmacenamiento(BigDecimal.ZERO)
                .build();
    }

    @Test
    void createContract_shouldCreateWith12Cuotas() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(contratoRepository.findByTenantIdAndEstado(tenantId, EstadoContrato.ACTIVE)).thenReturn(List.of());
        when(cuotaMensualRepository.findByTenantIdAndEstadoIn(eq(tenantId), anyList())).thenReturn(List.of());
        when(contratoRepository.save(any(ContratoAnual.class))).thenAnswer(i -> i.getArgument(0));
        when(cuotaMensualRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        CreateContractRequest request = new CreateContractRequest(
                TipoContrato.MODULO, "ADMIN", new BigDecimal("200.00"),
                LocalDate.of(2026, 7, 1), true);

        CommandResponse response = service.createContract(tenantId, request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getCorrelationId()).isNotNull();

        // Verify 12 cuotas were saved
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CuotaMensual>> cuotasCaptor = ArgumentCaptor.forClass(List.class);
        verify(cuotaMensualRepository).saveAll(cuotasCaptor.capture());
        List<CuotaMensual> savedCuotas = cuotasCaptor.getValue();
        assertThat(savedCuotas).hasSize(12);

        // Verify all cuotas have correct monto
        assertThat(savedCuotas).allMatch(c ->
                c.getMontoOriginal().compareTo(new BigDecimal("200.00")) == 0);

        // Verify all cuotas are PENDIENTE
        assertThat(savedCuotas).allMatch(c -> c.getEstado() == EstadoCuota.PENDIENTE);

        // Verify sequential numbering
        for (int i = 0; i < 12; i++) {
            assertThat(savedCuotas.get(i).getNumero()).isEqualTo(i + 1);
        }

        verify(cacheInvalidationService).invalidateAccessCache(tenantId);
    }

    @Test
    void createContract_duplicateModule_shouldThrow409() {
        ContratoAnual existing = ContratoAnual.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .tipo(TipoContrato.MODULO)
                .modulo("ADMIN")
                .estado(EstadoContrato.ACTIVE)
                .build();

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(contratoRepository.findByTenantIdAndEstado(tenantId, EstadoContrato.ACTIVE))
                .thenReturn(List.of(existing));

        CreateContractRequest request = new CreateContractRequest(
                TipoContrato.MODULO, "ADMIN", new BigDecimal("200.00"),
                LocalDate.of(2026, 7, 1), true);

        assertThatThrownBy(() -> service.createContract(tenantId, request))
                .isInstanceOf(DuplicateContractException.class);
    }

    @Test
    void createContract_withOverdueCuotas_shouldThrow422() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(contratoRepository.findByTenantIdAndEstado(tenantId, EstadoContrato.ACTIVE)).thenReturn(List.of());

        CuotaMensual overdue = CuotaMensual.builder()
                .id(UUID.randomUUID())
                .estado(EstadoCuota.VENCIDA)
                .montoOriginal(new BigDecimal("200.00"))
                .descuentoPct(BigDecimal.ZERO)
                .intentoCobro(0)
                .build();
        when(cuotaMensualRepository.findByTenantIdAndEstadoIn(eq(tenantId), anyList()))
                .thenReturn(List.of(overdue));

        CreateContractRequest request = new CreateContractRequest(
                TipoContrato.MODULO, "REPORTS", new BigDecimal("150.00"),
                LocalDate.of(2026, 7, 1), true);

        assertThatThrownBy(() -> service.createContract(tenantId, request))
                .isInstanceOf(OverdueCuotasException.class);
    }

    @Test
    void requestDowngrade_activeContract_shouldSetRenovacionAutoFalse() {
        UUID contratoId = UUID.randomUUID();
        ContratoAnual contrato = ContratoAnual.builder()
                .id(contratoId)
                .tenant(tenant)
                .estado(EstadoContrato.ACTIVE)
                .renovacionAuto(true)
                .build();

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(contratoRepository.findById(contratoId)).thenReturn(Optional.of(contrato));
        when(cuotaMensualRepository.findByTenantIdAndEstadoIn(eq(tenantId), anyList())).thenReturn(List.of());
        when(contratoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CommandResponse response = service.requestDowngrade(tenantId, contratoId);

        assertThat(response.getId()).isEqualTo(contratoId);
        assertThat(contrato.getRenovacionAuto()).isFalse();
        verify(cacheInvalidationService).invalidateAccessCache(tenantId);
    }

    @Test
    void requestDowngrade_nonActiveContract_shouldThrow() {
        UUID contratoId = UUID.randomUUID();
        ContratoAnual contrato = ContratoAnual.builder()
                .id(contratoId)
                .tenant(tenant)
                .estado(EstadoContrato.EXPIRED)
                .renovacionAuto(true)
                .build();

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(contratoRepository.findById(contratoId)).thenReturn(Optional.of(contrato));

        assertThatThrownBy(() -> service.requestDowngrade(tenantId, contratoId))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void renewContract_expiredContract_shouldCreateNewContract() {
        UUID oldContratoId = UUID.randomUUID();
        ContratoAnual oldContract = ContratoAnual.builder()
                .id(oldContratoId)
                .tenant(tenant)
                .tipo(TipoContrato.MODULO)
                .modulo("ADMIN")
                .estado(EstadoContrato.EXPIRED)
                .cuotaMensual(new BigDecimal("200.00"))
                .cuotasPagadas(12)
                .cuotasTotales(12)
                .fechaInicio(LocalDate.of(2025, 7, 1))
                .fechaVencimiento(LocalDate.of(2026, 7, 1))
                .fechaAniversario(1)
                .renovacionAuto(true)
                .creadoEn(LocalDateTime.now().minusYears(1))
                .build();

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(contratoRepository.findById(oldContratoId)).thenReturn(Optional.of(oldContract));
        when(contratoRepository.save(any(ContratoAnual.class))).thenAnswer(i -> i.getArgument(0));
        when(cuotaMensualRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        RenewContractRequest request = new RenewContractRequest("STRIPE");
        CommandResponse response = service.renewContract(tenantId, oldContratoId, request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getId()).isNotEqualTo(oldContratoId);

        // Verify new contract was saved
        verify(contratoRepository, times(1)).save(argThat(c ->
                c.getId() != null &&
                !c.getId().equals(oldContratoId) &&
                c.getFechaInicio().equals(LocalDate.of(2026, 7, 2)) &&
                c.getFechaVencimiento().equals(LocalDate.of(2027, 7, 2)) &&
                c.getEstado() == EstadoContrato.CREATED
        ));

        verify(cacheInvalidationService).invalidateAccessCache(tenantId);
    }

    @Test
    void renewContract_activeInWindow_shouldSucceed() {
        UUID oldContratoId = UUID.randomUUID();
        LocalDate vencimiento = LocalDate.now().plusDays(15); // within 30-day window
        ContratoAnual oldContract = ContratoAnual.builder()
                .id(oldContratoId)
                .tenant(tenant)
                .tipo(TipoContrato.MODULO)
                .modulo("ADMIN")
                .estado(EstadoContrato.ACTIVE)
                .cuotaMensual(new BigDecimal("200.00"))
                .cuotasPagadas(10)
                .cuotasTotales(12)
                .fechaInicio(vencimiento.minusMonths(12))
                .fechaVencimiento(vencimiento)
                .fechaAniversario(vencimiento.getDayOfMonth())
                .renovacionAuto(true)
                .creadoEn(LocalDateTime.now().minusMonths(11))
                .build();

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(contratoRepository.findById(oldContratoId)).thenReturn(Optional.of(oldContract));
        when(contratoRepository.save(any(ContratoAnual.class))).thenAnswer(i -> i.getArgument(0));
        when(cuotaMensualRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        RenewContractRequest request = new RenewContractRequest("STRIPE");
        CommandResponse response = service.renewContract(tenantId, oldContratoId, request);

        assertThat(response.getId()).isNotNull();
        // Old contract should now be EXPIRED
        assertThat(oldContract.getEstado()).isEqualTo(EstadoContrato.EXPIRED);
    }

    @Test
    void renewContract_outsideWindow_shouldThrow() {
        UUID oldContratoId = UUID.randomUUID();
        LocalDate vencimiento = LocalDate.now().plusDays(60); // outside window
        ContratoAnual oldContract = ContratoAnual.builder()
                .id(oldContratoId)
                .tenant(tenant)
                .tipo(TipoContrato.MODULO)
                .modulo("ADMIN")
                .estado(EstadoContrato.ACTIVE)
                .cuotaMensual(new BigDecimal("200.00"))
                .cuotasPagadas(8)
                .cuotasTotales(12)
                .fechaInicio(vencimiento.minusMonths(12))
                .fechaVencimiento(vencimiento)
                .fechaAniversario(1)
                .renovacionAuto(true)
                .creadoEn(LocalDateTime.now().minusMonths(8))
                .build();

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(contratoRepository.findById(oldContratoId)).thenReturn(Optional.of(oldContract));

        RenewContractRequest request = new RenewContractRequest("STRIPE");
        assertThatThrownBy(() -> service.renewContract(tenantId, oldContratoId, request))
                .isInstanceOf(RenewalNotAllowedException.class);
    }
}

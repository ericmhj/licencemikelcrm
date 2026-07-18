package com.mikelcrm.licenseservice.controller;

import com.mikelcrm.licenseservice.domain.entity.Plan;
import com.mikelcrm.licenseservice.domain.repository.PlanRepository;
import com.mikelcrm.licenseservice.security.rbac.RequiresPermission;
import com.mikelcrm.licenseservice.service.cache.PlanCacheWarmupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanRepository planRepository;
    private final PlanCacheWarmupService planCacheWarmupService;

    /**
     * GET /api/v1/plans — List all active plans (public)
     */
    @GetMapping
    public ResponseEntity<List<Plan>> listPlans() {
        return ResponseEntity.ok(planRepository.findByActivoTrue());
    }

    /**
     * GET /api/v1/plans/{codigo} — Get plan details (public)
     */
    @GetMapping("/{codigo}")
    public ResponseEntity<Plan> getPlan(@PathVariable String codigo) {
        return planRepository.findByCodigo(codigo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/v1/plans/{codigo}/limits — Get plan limits for consumption control
     */
    @GetMapping("/{codigo}/limits")
    public ResponseEntity<Map<String, Object>> getPlanLimits(@PathVariable String codigo) {
        return planRepository.findByCodigo(codigo)
                .map(plan -> {
                    String rolesJson = plan.getRolesAutorizados();
                    List<String> roles = (rolesJson != null && !rolesJson.isBlank())
                            ? List.of(rolesJson.replace("[", "").replace("]", "").replace("\"", "").split(",\\s*"))
                            : List.of();
                    return ResponseEntity.ok(Map.<String, Object>of(
                            "codigo", plan.getCodigo(),
                            "creditos_mensuales", plan.getCreditosMensuales(),
                            "max_free_downloads", plan.getMaxFreeDownloads(),
                            "max_usuarios", plan.getMaxUsuarios(),
                            "roles_autorizados", roles
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/v1/plans — Create a new plan (requires platform admin)
     */
    @PostMapping
    @RequiresPermission(recurso = "planes", accion = "crear")
    public ResponseEntity<Plan> createPlan(@Valid @RequestBody CreatePlanRequest request) {
        if (planRepository.findByCodigo(request.codigo()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        Plan plan = Plan.builder()
                .codigo(request.codigo())
                .nombre(request.nombre())
                .descripcion(request.descripcion())
                .creditosMensuales(request.creditosMensuales())
                .maxFreeDownloads(request.maxFreeDownloads())
                .maxUsuarios(request.maxUsuarios())
                .rolesAutorizados(request.rolesAutorizados() != null ? "[\"" + String.join("\",\"", request.rolesAutorizados()) + "\"]" : "[]")
                .precioMensual(request.precioMensual())
                .activo(true)
                .build();

        Plan saved = planRepository.save(plan);
        planCacheWarmupService.syncPlanToRedis(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * PUT /api/v1/plans/{codigo} — Update an existing plan
     */
    @PutMapping("/{codigo}")
    @RequiresPermission(recurso = "planes", accion = "actualizar")
    public ResponseEntity<Plan> updatePlan(
            @PathVariable String codigo,
            @Valid @RequestBody UpdatePlanRequest request) {
        return planRepository.findByCodigo(codigo)
                .map(plan -> {
                    if (request.nombre() != null) plan.setNombre(request.nombre());
                    if (request.descripcion() != null) plan.setDescripcion(request.descripcion());
                    if (request.creditosMensuales() != null) plan.setCreditosMensuales(request.creditosMensuales());
                    if (request.maxFreeDownloads() != null) plan.setMaxFreeDownloads(request.maxFreeDownloads());
                    if (request.maxUsuarios() != null) plan.setMaxUsuarios(request.maxUsuarios());
                    if (request.rolesAutorizados() != null) plan.setRolesAutorizados("[\"" + String.join("\",\"", request.rolesAutorizados()) + "\"]");
                    if (request.precioMensual() != null) plan.setPrecioMensual(request.precioMensual());
                    planRepository.save(plan);
                    planCacheWarmupService.syncPlanToRedis(plan);
                    return ResponseEntity.ok(plan);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/v1/plans/{codigo} — Soft delete (deactivate) a plan
     */
    @DeleteMapping("/{codigo}")
    @RequiresPermission(recurso = "planes", accion = "eliminar")
    public ResponseEntity<Void> deactivatePlan(@PathVariable String codigo) {
        return planRepository.findByCodigo(codigo)
                .map(plan -> {
                    plan.setActivo(false);
                    planRepository.save(plan);
                    planCacheWarmupService.removePlanFromRedis(codigo);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // --- DTOs ---

    public record CreatePlanRequest(
            String codigo,
            String nombre,
            String descripcion,
            Integer creditosMensuales,
            Integer maxFreeDownloads,
            Integer maxUsuarios,
            List<String> rolesAutorizados,
            java.math.BigDecimal precioMensual
    ) {}

    public record UpdatePlanRequest(
            String nombre,
            String descripcion,
            Integer creditosMensuales,
            Integer maxFreeDownloads,
            Integer maxUsuarios,
            List<String> rolesAutorizados,
            java.math.BigDecimal precioMensual
    ) {}
}

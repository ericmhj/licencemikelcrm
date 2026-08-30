package com.mikelcrm.licenseservice.controller;

import com.mikelcrm.licenseservice.controller.dto.CreateFuncionRolesRequest;
import com.mikelcrm.licenseservice.controller.dto.FuncionRolesDTO;
import com.mikelcrm.licenseservice.controller.dto.UpdateFuncionRolesRequest;
import com.mikelcrm.licenseservice.security.rbac.RequiresPermission;
import com.mikelcrm.licenseservice.service.FuncionRolesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/funcion-roles")
@RequiredArgsConstructor
public class FuncionRolesController {

    private final FuncionRolesService funcionRolesService;

    /**
     * GET /api/v1/funcion-roles — List all funcion-roles relationships
     */
    @GetMapping
    public ResponseEntity<List<FuncionRolesDTO>> listAll() {
        List<FuncionRolesDTO> result = funcionRolesService.findAll();
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/v1/funcion-roles/{codigo} — Get a funcion-roles relationship by codigo
     */
    @GetMapping("/{codigo}")
    public ResponseEntity<FuncionRolesDTO> getByCodigo(@PathVariable String codigo) {
        FuncionRolesDTO result = funcionRolesService.findByCodigo(codigo);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/v1/funcion-roles — Create a new funcion-roles relationship
     */
    @PostMapping
    @RequiresPermission(recurso = "planes", accion = "crear")
    public ResponseEntity<FuncionRolesDTO> create(@Valid @RequestBody CreateFuncionRolesRequest request) {
        FuncionRolesDTO created = funcionRolesService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/v1/funcion-roles/{codigo} — Update an existing funcion-roles relationship
     */
    @PutMapping("/{codigo}")
    @RequiresPermission(recurso = "planes", accion = "actualizar")
    public ResponseEntity<FuncionRolesDTO> update(
            @PathVariable String codigo,
            @Valid @RequestBody UpdateFuncionRolesRequest request) {
        FuncionRolesDTO updated = funcionRolesService.update(codigo, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/v1/funcion-roles/{codigo} — Delete a funcion-roles relationship
     */
    @DeleteMapping("/{codigo}")
    @RequiresPermission(recurso = "planes", accion = "eliminar")
    public ResponseEntity<Void> delete(@PathVariable String codigo) {
        funcionRolesService.delete(codigo);
        return ResponseEntity.noContent().build();
    }
}

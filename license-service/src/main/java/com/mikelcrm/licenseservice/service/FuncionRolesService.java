package com.mikelcrm.licenseservice.service;

import com.mikelcrm.licenseservice.controller.dto.CreateFuncionRolesRequest;
import com.mikelcrm.licenseservice.controller.dto.FuncionRolesDTO;
import com.mikelcrm.licenseservice.controller.dto.UpdateFuncionRolesRequest;
import com.mikelcrm.licenseservice.domain.entity.FuncionRoles;
import com.mikelcrm.licenseservice.domain.repository.FuncionRolesRepository;
import com.mikelcrm.licenseservice.domain.validation.RoleSetValidator;
import com.mikelcrm.licenseservice.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FuncionRolesService {

    private final FuncionRolesRepository repository;

    /**
     * Returns all funcion-roles relationships mapped to DTOs.
     */
    @Transactional(readOnly = true)
    public List<FuncionRolesDTO> findAll() {
        return repository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Finds a funcion-roles entity by its codigo.
     *
     * @throws FuncionRolesNotFoundException if the codigo does not exist
     */
    @Transactional(readOnly = true)
    public FuncionRolesDTO findByCodigo(String codigo) {
        FuncionRoles entity = repository.findByCodigo(codigo)
                .orElseThrow(() -> new FuncionRolesNotFoundException(codigo));
        return toDTO(entity);
    }

    /**
     * Creates a new funcion-roles relationship.
     *
     * @throws EmptyRolesException if roles list is empty or null
     * @throws InvalidRoleException if any role is not in VALID_ROLES
     * @throws DuplicateCodigoException if codigo already exists
     * @throws DuplicateRoleSetException if the role set matches an existing entity
     */
    @Transactional
    public FuncionRolesDTO create(CreateFuncionRolesRequest request) {
        validateRoles(request.roles());

        // Check for duplicate codigo
        repository.findByCodigo(request.codigo()).ifPresent(existing -> {
            throw new DuplicateCodigoException(request.codigo());
        });

        // Check role set uniqueness
        checkRoleSetUniqueness(request.roles(), null);

        // Normalize roles (sort) before storing
        List<String> normalizedRoles = normalizRoles(request.roles());

        FuncionRoles entity = FuncionRoles.builder()
                .codigo(request.codigo())
                .descripcion(request.descripcion())
                .funcionalidad(request.funcionalidad())
                .roles(normalizedRoles)
                .build();

        FuncionRoles saved = repository.save(entity);
        log.info("Created funcion-roles with codigo: {}", saved.getCodigo());
        return toDTO(saved);
    }

    /**
     * Updates an existing funcion-roles relationship.
     *
     * @throws FuncionRolesNotFoundException if the codigo does not exist
     * @throws EmptyRolesException if roles list is empty or null
     * @throws InvalidRoleException if any role is not in VALID_ROLES
     * @throws DuplicateRoleSetException if the role set matches another existing entity
     */
    @Transactional
    public FuncionRolesDTO update(String codigo, UpdateFuncionRolesRequest request) {
        FuncionRoles entity = repository.findByCodigo(codigo)
                .orElseThrow(() -> new FuncionRolesNotFoundException(codigo));

        validateRoles(request.roles());

        // Check role set uniqueness, excluding self
        checkRoleSetUniqueness(request.roles(), codigo);

        // Normalize roles (sort) before storing
        List<String> normalizedRoles = normalizRoles(request.roles());

        entity.setDescripcion(request.descripcion());
        entity.setFuncionalidad(request.funcionalidad());
        entity.setRoles(normalizedRoles);

        FuncionRoles saved = repository.save(entity);
        log.info("Updated funcion-roles with codigo: {}", saved.getCodigo());
        return toDTO(saved);
    }

    /**
     * Deletes a funcion-roles relationship by codigo.
     *
     * @throws FuncionRolesNotFoundException if the codigo does not exist
     */
    @Transactional
    public void delete(String codigo) {
        FuncionRoles entity = repository.findByCodigo(codigo)
                .orElseThrow(() -> new FuncionRolesNotFoundException(codigo));
        repository.delete(entity);
        log.info("Deleted funcion-roles with codigo: {}", codigo);
    }

    // --- Private helper methods ---

    /**
     * Validates that roles list is non-empty and all roles are in VALID_ROLES.
     */
    private void validateRoles(List<String> roles) {
        if (!RoleSetValidator.isNonEmpty(roles)) {
            throw new EmptyRolesException();
        }

        if (!RoleSetValidator.allRolesValid(roles)) {
            List<String> invalidRoles = roles.stream()
                    .filter(role -> !RoleSetValidator.VALID_ROLES.contains(role))
                    .toList();
            throw new InvalidRoleException(invalidRoles);
        }
    }

    /**
     * Checks that no other existing entity has the same role set.
     * If excludeCodigo is provided, that entity is excluded from the comparison (for update scenarios).
     */
    private void checkRoleSetUniqueness(List<String> roles, String excludeCodigo) {
        List<FuncionRoles> allEntities = repository.findAll();

        for (FuncionRoles existing : allEntities) {
            // Skip the entity being updated
            if (excludeCodigo != null && excludeCodigo.equals(existing.getCodigo())) {
                continue;
            }

            if (RoleSetValidator.areRoleSetsEqual(roles, existing.getRoles())) {
                throw new DuplicateRoleSetException(existing.getCodigo());
            }
        }
    }

    /**
     * Normalizes a roles list by sorting it alphabetically.
     */
    private List<String> normalizRoles(List<String> roles) {
        List<String> sorted = new ArrayList<>(roles);
        Collections.sort(sorted);
        return sorted;
    }

    /**
     * Maps a FuncionRoles entity to a FuncionRolesDTO.
     */
    private FuncionRolesDTO toDTO(FuncionRoles entity) {
        LocalDateTime createdAt = entity.getCreatedAt() != null
                ? LocalDateTime.ofInstant(entity.getCreatedAt(), ZoneId.systemDefault())
                : null;
        LocalDateTime updatedAt = entity.getUpdatedAt() != null
                ? LocalDateTime.ofInstant(entity.getUpdatedAt(), ZoneId.systemDefault())
                : null;

        return new FuncionRolesDTO(
                entity.getId(),
                entity.getCodigo(),
                entity.getDescripcion(),
                entity.getFuncionalidad(),
                entity.getRoles(),
                createdAt,
                updatedAt
        );
    }
}

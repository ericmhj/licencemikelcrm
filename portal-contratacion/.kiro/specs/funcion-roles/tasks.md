# Implementation Plan: Funcion-Roles CRUD Module

## Overview

This plan implements the full "Funcion-Roles" CRUD module for the CRM admin platform. It covers the backend (Java Spring Boot with JPA/PostgreSQL) and frontend (Angular 18 with Angular Material), plus the admin layout redesign with sidebar navigation. Tasks are ordered to build foundational components first, then wire everything together.

## Tasks

- [x] 1. Backend entity, repository, and database setup
  - [x] 1.1 Create the FuncionRoles JPA entity and RolesJsonConverter
    - Create `FuncionRoles.java` entity class with fields: id (UUID), codigo, descripcion, funcionalidad, roles (List<String>), createdAt, updatedAt
    - Create `RolesJsonConverter.java` AttributeConverter that serializes roles as a sorted JSON array string and deserializes back to List<String>
    - Add @PrePersist and @PreUpdate lifecycle callbacks for timestamps
    - _Requirements: 1.1, 1.2, 1.3_

  - [x] 1.2 Create the FuncionRolesRepository interface
    - Define a Spring Data JPA repository for FuncionRoles
    - Add a `findByCodigo(String codigo)` query method returning Optional<FuncionRoles>
    - _Requirements: 1.1_

  - [x] 1.3 Create DTO records and request validation
    - Create `CreateFuncionRolesRequest` record with @NotBlank codigo, @NotBlank funcionalidad, @NotEmpty roles, and optional descripcion
    - Create `UpdateFuncionRolesRequest` record with @NotBlank funcionalidad, @NotEmpty roles, and optional descripcion
    - Create `FuncionRolesDTO` response record with all entity fields
    - _Requirements: 3.1, 4.1_

- [x] 2. Backend validation and service layer
  - [x] 2.1 Implement RoleSetValidator utility class
    - Create `RoleSetValidator.java` with static VALID_ROLES set (tecnico, asistente, manager, admin, superusuario)
    - Implement `allRolesValid(List<String>)` — returns true only if every element is in VALID_ROLES
    - Implement `isNonEmpty(List<String>)` — returns true if list is non-null and non-empty
    - Implement `areRoleSetsEqual(List<String>, List<String>)` — compares as sorted sets for order-independent equality
    - _Requirements: 3.4, 3.5, 4.4, 4.5, 11.1_

  - [x]* 2.2 Write property test: Roles JSON serialization round-trip
    - **Property 1: Round-trip consistency**
    - Generate random subsets of Valid_Roles_Set, serialize to JSON, deserialize, and verify set equality
    - **Validates: Requirements 1.3**

  - [x]* 2.3 Write property test: Order-independent role set equality
    - **Property 2: Order-independent role set equality**
    - Generate random permutations of role lists and verify areRoleSetsEqual returns true; generate differing lists and verify it returns false
    - **Validates: Requirements 3.3, 4.3, 11.1**

  - [x]* 2.4 Write property test: Invalid roles are always rejected
    - **Property 3: Invalid roles are always rejected**
    - Generate random strings not in Valid_Roles_Set, mix with valid roles, and verify allRolesValid returns false
    - **Validates: Requirements 3.5, 4.5**

  - [x] 2.5 Implement FuncionRolesService backend service
    - Create `FuncionRolesService.java` with methods: findAll, findByCodigo, create, update, delete
    - Implement role validation (empty check + valid roles check) throwing appropriate exceptions for 400 errors
    - Implement role set uniqueness check: normalize via sort, compare against all existing records, exclude self on update
    - Map entities to DTOs for responses
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 4.1, 4.2, 4.3, 4.4, 4.5, 5.1, 5.2, 11.1, 11.2, 11.3, 11.4_

  - [x]* 2.6 Write property test: Uniqueness check self-exclusion on update
    - **Property 4: Uniqueness check self-exclusion on update**
    - Given an existing entity, verify that updating with its own role set passes uniqueness validation
    - **Validates: Requirements 4.3, 11.2**

- [x] 3. Backend REST controller and security
  - [x] 3.1 Implement FuncionRolesController
    - Create `FuncionRolesController.java` with @RestController and @RequestMapping("/api/v1/funcion-roles")
    - Implement GET (list all, 200), GET by codigo (200/404), POST (create, 201/400/409), PUT by codigo (update, 200/400/404/409), DELETE by codigo (204/404)
    - Wire to FuncionRolesService and apply @Valid on request body parameters
    - Return consistent error JSON structure with error code, message, and optional conflictingCodigo
    - _Requirements: 2.1, 2.2, 2.3, 3.1, 3.2, 3.3, 3.4, 3.5, 4.1, 4.2, 4.3, 4.4, 4.5, 5.1, 5.2_

  - [x] 3.2 Configure security permitAll for funcion-roles endpoints
    - Update SecurityConfig to add `/api/v1/funcion-roles` and `/api/v1/funcion-roles/**` to the permitAll list
    - _Requirements: 6.1_

  - [x]* 3.3 Write unit tests for FuncionRolesController
    - Test GET list (200), GET by code (200/404), POST (201/400/409), PUT (200/400/404/409), DELETE (204/404)
    - Use MockMvc with mocked service layer
    - _Requirements: 2.1, 2.2, 2.3, 3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 5.1, 5.2_

- [x] 4. Checkpoint - Backend complete
  - Ensure all backend tests pass, ask the user if questions arise.

- [x] 5. Frontend service and models
  - [x] 5.1 Create TypeScript interfaces and models
    - Create `funcion-roles.model.ts` with interfaces: FuncionRoles, CreateFuncionRolesRequest, UpdateFuncionRolesRequest
    - Define the VALID_ROLES constant array for use in form checkboxes
    - _Requirements: 7.1_

  - [x] 5.2 Implement FuncionRolesService (Angular)
    - Create `funcion-roles.service.ts` as standalone injectable service
    - Implement methods: getAll(), getByCode(codigo), create(request), update(codigo, request), delete(codigo)
    - Use HttpClient with base URL from environment configuration
    - Return Observable types, propagate HTTP errors as Observable errors with meaningful messages
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

- [x] 6. Frontend list and form components
  - [x] 6.1 Implement FuncionRolesListComponent
    - Create standalone component with Angular Material table
    - Display columns: Codigo, Descripcion, Funcionalidad, Roles (as Material chips), Acciones (edit/delete buttons)
    - Add "Nueva Funcion-Roles" button navigating to `/admin/funcion_roles/nuevo`
    - Implement delete with confirmation dialog, display error snackbar on failure
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7_

  - [x] 6.2 Implement FuncionRolesFormComponent
    - Create standalone dual-mode component (create/edit) based on URL context (presence of `:codigo` param)
    - Fields: codigo (text input, disabled in edit mode based on URL), descripcion (textarea), funcionalidad (text input), role checkboxes for all Valid_Roles_Set
    - Validate at least one role selected before submission
    - On success: display snackbar and navigate to list
    - On 409: display specific snackbar (duplicate codigo or duplicate role set with conflicting codigo)
    - On network error: display connectivity error snackbar, preserve form state
    - Cancel button navigates back to list without saving
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 9.8_

  - [x]* 6.3 Write unit tests for FuncionRolesListComponent
    - Test table rendering, chip display, navigation on edit click, delete confirmation flow, error snackbar on delete failure
    - _Requirements: 8.1, 8.2, 8.3, 8.5, 8.7_

  - [x]* 6.4 Write unit tests for FuncionRolesFormComponent
    - Test create mode (empty form), edit mode (pre-populated, codigo disabled), role validation, 409 handling, cancel navigation
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 9.8_

- [x] 7. Admin layout and sidebar navigation
  - [x] 7.1 Implement AdminLayoutComponent with sidebar
    - Create standalone AdminLayoutComponent with two-panel layout: left sidebar + right router-outlet
    - Display "Administrador de CRM" title in header
    - Add logout button in header that clears session and redirects to `/login`
    - _Requirements: 12.1, 12.6_

  - [x] 7.2 Implement AdminSidebar navigation
    - Add sidebar links: "Planes" → `/admin/planes`, "Funcion-Roles" → `/admin/funcion_roles`
    - Apply routerLinkActive for active module highlighting
    - Handle router navigation; allow full page reload as fallback on navigation failure
    - _Requirements: 12.2, 12.3, 12.4, 12.5, 12.7, 12.8_

- [x] 8. Frontend routing and login integration
  - [x] 8.1 Configure routing for funcion-roles module
    - Register route `/admin/funcion_roles` → FuncionRolesListComponent
    - Register route `/admin/funcion_roles/nuevo` → FuncionRolesFormComponent
    - Register route `/admin/funcion_roles/editar/:codigo` → FuncionRolesFormComponent
    - Protect all routes with authGuard and roleGuard (admin, superusuario, platform_admin)
    - Ensure direct URL navigation works after authentication
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

  - [x] 8.2 Update login page and post-login redirect
    - Change login page title to "Administrador de CRM"
    - After successful login with platform_admin/admin/superusuario role, redirect to `/admin/planes`
    - Wire AdminLayoutComponent as parent route for `/admin` path
    - _Requirements: 13.1, 13.2, 13.3_

- [x] 9. Integration wiring and final verification
  - [x] 9.1 Wire all frontend components and verify end-to-end flow
    - Ensure lazy loading of funcion-roles components
    - Verify navigation from sidebar to list, list to form, form back to list
    - Verify CRUD operations work against the backend API
    - _Requirements: 7.1, 8.1, 9.1, 10.1, 12.3_

  - [x]* 9.2 Write integration tests for full CRUD flow
    - Test create → read → update → read → delete → verify 404 sequence
    - Verify security (endpoints accessible without auth token)
    - _Requirements: 2.1, 3.1, 4.1, 5.1, 6.1_

- [x] 10. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- Backend tasks (1-3) are independent of frontend tasks (5-8) and can be developed in parallel after shared interfaces are defined
- The admin layout (task 7) can be developed in parallel with the CRUD components (task 6)

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.3", "5.1"] },
    { "id": 1, "tasks": ["1.2", "2.1", "5.2"] },
    { "id": 2, "tasks": ["2.2", "2.3", "2.4", "2.5", "7.1", "7.2"] },
    { "id": 3, "tasks": ["2.6", "3.1", "6.1", "6.2"] },
    { "id": 4, "tasks": ["3.2", "3.3", "6.3", "6.4"] },
    { "id": 5, "tasks": ["8.1", "8.2"] },
    { "id": 6, "tasks": ["9.1"] },
    { "id": 7, "tasks": ["9.2"] }
  ]
}
```

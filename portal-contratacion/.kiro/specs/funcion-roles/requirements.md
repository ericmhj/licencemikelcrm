# Requirements Document

## Introduction

This feature transforms the Portal de Contratación into an "Administrador de CRM" platform. After login, the administrator sees a two-panel layout: a left sidebar with the available modules (Planes, Funcion-Roles) and the selected module's view on the right. The login screen is shared — it's the same Keycloak authentication with platform_admin credentials. This spec covers the new "Funcion-Roles" CRUD module and the admin layout redesign.

## Glossary

- **Funcion_Roles_Module**: The admin CRUD module for managing funcion-roles relationships, accessible at `/admin/funcion_roles`
- **Funcion_Roles_Entity**: A database entity representing a named grouping that associates a set of roles to a feature capability
- **Funcion_Roles_API**: The REST API at `/api/v1/funcion-roles` that handles CRUD operations for funcion-roles entities
- **Funcion_Roles_Service**: The Angular service that communicates with the Funcion_Roles_API
- **Funcion_Roles_List**: The list page component that displays all funcion-roles relationships in a Material table
- **Funcion_Roles_Form**: The form component used for creating and editing funcion-roles relationships
- **Valid_Roles_Set**: The fixed set of realm roles: tecnico, asistente, manager, admin, superusuario
- **Role_Set_Uniqueness**: A business constraint that prevents two different funcion-roles relationships from having the exact same combination of roles
- **Codigo**: A unique text identifier for a funcion-roles relationship (e.g., "FUNC_BASICA")
- **Funcionalidad**: A free-text field describing the feature capability name associated with the role grouping
- **License_Service**: The Java Spring Boot backend service with PostgreSQL database
- **Portal_Contratacion**: The Angular 17+ frontend application
- **Admin_Layout**: The two-panel layout shown after login — a left sidebar listing admin modules and the selected module's content on the right
- **Admin_Sidebar**: The left column navigation listing available modules (Planes, Funcion-Roles)
- **CRM_Admin_Title**: The application title "Administrador de CRM" displayed in the layout header

## Requirements

### Requirement 1: Backend Entity and Database Table

**User Story:** As a platform administrator, I want funcion-roles relationships stored persistently in the database, so that role groupings survive application restarts and are shared across instances.

#### Acceptance Criteria

1. THE License_Service SHALL define a `funcion_roles` database table with columns: id (UUID, primary key), codigo (VARCHAR, unique, not null), descripcion (TEXT), funcionalidad (VARCHAR, not null), roles (JSON array, not null), and created_at/updated_at timestamps
2. THE Funcion_Roles_Entity SHALL require successful primary key constraint configuration before JPA entity mapping is applied to the `funcion_roles` table
3. THE Funcion_Roles_Entity SHALL store the roles field as a JSON array string in the database

### Requirement 2: REST API - List and Get

**User Story:** As a frontend application, I want to retrieve funcion-roles relationships from the backend, so that I can display them to the administrator.

#### Acceptance Criteria

1. WHEN a GET request is sent to `/api/v1/funcion-roles`, THE Funcion_Roles_API SHALL return a JSON array containing all funcion-roles relationships with HTTP status 200
2. WHEN a GET request is sent to `/api/v1/funcion-roles/{codigo}` with a valid codigo, THE Funcion_Roles_API SHALL return the matching funcion-roles entity with HTTP status 200
3. WHEN a GET request is sent to `/api/v1/funcion-roles/{codigo}` with a non-existent codigo, THE Funcion_Roles_API SHALL return HTTP status 404 with an error object in the response body and SHALL NOT return any entity data

### Requirement 3: REST API - Create

**User Story:** As a platform administrator, I want to create new funcion-roles relationships via the API, so that I can define new role groupings for features.

#### Acceptance Criteria

1. WHEN a POST request with valid payload is sent to `/api/v1/funcion-roles`, THE Funcion_Roles_API SHALL create a new entity and return it with HTTP status 201
2. WHEN a POST request contains a codigo that already exists, THE Funcion_Roles_API SHALL reject the request with HTTP status 409
3. WHEN a POST request contains a roles set that is identical to an existing relationship's roles set, THE Funcion_Roles_API SHALL reject the request with HTTP status 409 and an error message indicating duplicate role set
4. WHEN a POST request contains an empty roles array, THE Funcion_Roles_API SHALL reject the request with HTTP status 400
5. WHEN a POST request contains roles not in the Valid_Roles_Set, THE Funcion_Roles_API SHALL reject the request with HTTP status 400

### Requirement 4: REST API - Update

**User Story:** As a platform administrator, I want to update existing funcion-roles relationships, so that I can modify role groupings as business needs change.

#### Acceptance Criteria

1. WHEN a PUT request with valid payload is sent to `/api/v1/funcion-roles/{codigo}`, THE Funcion_Roles_API SHALL update the existing entity and return it with HTTP status 200
2. WHEN a PUT request is sent to a non-existent codigo, THE Funcion_Roles_API SHALL return HTTP status 404
3. WHEN a PUT request contains a roles set identical to another existing relationship's roles set (excluding the entity being updated), THE Funcion_Roles_API SHALL reject the request with HTTP status 409
4. WHEN a PUT request contains an empty roles array, THE Funcion_Roles_API SHALL reject the request with HTTP status 400
5. WHEN a PUT request contains roles not in the Valid_Roles_Set, THE Funcion_Roles_API SHALL reject the request with HTTP status 400

### Requirement 5: REST API - Delete

**User Story:** As a platform administrator, I want to delete funcion-roles relationships, so that I can remove obsolete role groupings.

#### Acceptance Criteria

1. WHEN a DELETE request is sent to `/api/v1/funcion-roles/{codigo}` with a valid codigo, THE Funcion_Roles_API SHALL remove the entity and return HTTP status 204
2. WHEN a DELETE request is sent to a non-existent codigo, THE Funcion_Roles_API SHALL return HTTP status 404 (deletion is not idempotent — non-existent resources are treated as errors)

### Requirement 6: API Security Configuration

**User Story:** As a platform administrator, I want the funcion-roles API endpoints to be publicly accessible (same as plans), so that the admin frontend can call them without gateway authentication.

#### Acceptance Criteria

1. THE License_Service SHALL configure the `/api/v1/funcion-roles` and `/api/v1/funcion-roles/**` endpoints as `permitAll` in the SecurityConfig filter chain; all operations including role assignment and deletion SHALL remain publicly accessible without authentication

### Requirement 7: Frontend Service

**User Story:** As a developer, I want an Angular service that abstracts API communication, so that components can interact with the backend through a clean interface.

#### Acceptance Criteria

1. THE Funcion_Roles_Service SHALL expose methods for: getAll, getByCode, create, update, and delete operations
2. THE Funcion_Roles_Service SHALL communicate with the Funcion_Roles_API using the base URL from environment configuration; IF the environment configuration is missing or invalid, THE service SHALL fail fast with an explicit error
3. THE Funcion_Roles_Service SHALL return Observable types for all API operations
4. THE Funcion_Roles_Service SHALL handle HTTP errors by propagating them as Observable errors with meaningful error messages to the calling component

### Requirement 8: List Page

**User Story:** As a platform administrator, I want to see all funcion-roles relationships in a table, so that I can review and manage existing role groupings.

#### Acceptance Criteria

1. WHEN the administrator navigates to `/admin/funcion_roles`, THE Funcion_Roles_List SHALL display a Material table with columns: Codigo, Descripcion, Funcionalidad, Roles, and Acciones
2. THE Funcion_Roles_List SHALL display the roles column as Material chips
3. THE Funcion_Roles_List SHALL provide an edit button in the Acciones column that navigates to `/admin/funcion_roles/editar/{codigo}`
4. WHEN navigation to the edit page fails, THE Funcion_Roles_List SHALL display error feedback to the administrator
5. THE Funcion_Roles_List SHALL provide a delete button in the Acciones column that triggers a confirmation dialog before deleting
6. THE Funcion_Roles_List SHALL display a "Nueva Funcion-Roles" button that navigates to `/admin/funcion_roles/nuevo`
7. WHEN a delete operation fails, THE Funcion_Roles_List SHALL display a snackbar notification with the error message

### Requirement 9: Create/Edit Form Page

**User Story:** As a platform administrator, I want a form to create and edit funcion-roles relationships, so that I can define or modify role groupings with validated inputs.

#### Acceptance Criteria

1. WHEN the administrator navigates to `/admin/funcion_roles/nuevo`, THE Funcion_Roles_Form SHALL display an empty form with fields: codigo, descripcion, funcionalidad (free text), and role checkboxes for all Valid_Roles_Set roles
2. WHEN the administrator navigates to `/admin/funcion_roles/editar/{codigo}`, THE Funcion_Roles_Form SHALL explicitly set the form mode to EDIT and pre-populate the form with the existing entity data
3. WHEN the administrator navigates to an edit URL (`/admin/funcion_roles/editar/{codigo}`), THE Funcion_Roles_Form SHALL always disable the codigo field based on the URL context, regardless of the internal form mode state
4. WHEN the administrator submits the form without selecting at least one role, THE Funcion_Roles_Form SHALL prevent submission and display a validation message
5. WHEN the form is submitted successfully, THE Funcion_Roles_Form SHALL display a success snackbar and navigate back to the list page
6. WHEN the API returns a 409 error on submission, THE Funcion_Roles_Form SHALL display a snackbar indicating either duplicate codigo or duplicate role set
7. THE Funcion_Roles_Form SHALL provide a cancel button that navigates back to the list page without saving
8. WHEN the Funcion_Roles_Form encounters a network error during submission, THE form SHALL display a snackbar with a connectivity error message and remain on the form page preserving user input

### Requirement 10: Frontend Routing

**User Story:** As a developer, I want the funcion-roles module integrated into the admin routing structure, so that administrators can navigate to it using the standard admin navigation.

#### Acceptance Criteria

1. THE Portal_Contratacion SHALL register the route `/admin/funcion_roles` to load the Funcion_Roles_List component
2. THE Portal_Contratacion SHALL register the route `/admin/funcion_roles/nuevo` to load the Funcion_Roles_Form component
3. THE Portal_Contratacion SHALL register the route `/admin/funcion_roles/editar/:codigo` to load the Funcion_Roles_Form component
4. THE Portal_Contratacion SHALL protect all funcion_roles routes with the existing authGuard and roleGuard requiring roles: admin, superusuario, or platform_admin
5. WHEN a user navigates directly to a funcion_roles URL (e.g., via bookmark or page refresh), THE Portal_Contratacion SHALL load the corresponding component normally after authentication

### Requirement 11: Role Set Uniqueness Validation

**User Story:** As a platform administrator, I want the system to prevent duplicate role sets, so that each funcion-roles relationship represents a unique combination of roles.

#### Acceptance Criteria

1. WHEN validating role set uniqueness, THE Funcion_Roles_API SHALL compare role sets regardless of order (e.g., [admin, tecnico] is considered equal to [tecnico, admin])
2. THE Funcion_Roles_API SHALL perform uniqueness validation on both create and update operations
3. WHEN a duplicate role set is detected, THE Funcion_Roles_API SHALL reject the operation and SHALL always include the codigo of the conflicting existing relationship in the error response when retrieval succeeds; IF retrieval of the conflicting codigo fails, THE system SHALL still reject the operation without the conflicting codigo
4. THE Funcion_Roles_API SHALL reject operations when any validation error occurs, not limited to duplicate role set detection

### Requirement 12: Admin Layout with Sidebar Navigation

**User Story:** As a platform administrator, I want a two-panel admin layout after login, so that I can navigate between modules (Planes, Funcion-Roles) without leaving the admin area.

#### Acceptance Criteria

1. WHEN an administrator logs in successfully, THE Portal_Contratacion SHALL display the Admin_Layout with the title "Administrador de CRM" in the header
2. THE Admin_Layout SHALL display a left sidebar (Admin_Sidebar) listing the available admin modules: "Planes" and "Funcion-Roles"
3. WHEN the administrator clicks a module in the Admin_Sidebar, THE Admin_Layout SHALL navigate to the corresponding module route using Angular Router, loading content without a full page reload
4. IF Angular Router navigation fails, THE system SHALL allow a full page reload as a fallback
5. THE Admin_Sidebar SHALL highlight the currently active module based on the current route
6. THE Admin_Layout SHALL include a logout button in the header that automatically clears the session and redirects to `/login`; automatic logout actions SHALL be permitted without requiring explicit user button clicks
7. THE Admin_Sidebar link "Planes" SHALL navigate to `/admin/planes`
8. THE Admin_Sidebar link "Funcion-Roles" SHALL navigate to `/admin/funcion_roles`

### Requirement 13: Shared Login Page

**User Story:** As a platform administrator, I want a single login page for the entire CRM admin, so that I authenticate once and can access all admin modules.

#### Acceptance Criteria

1. THE Portal_Contratacion SHALL use the existing `/login` route for authentication using Keycloak Direct Access Grant
2. WHEN login is successful with a platform_admin, admin, or superusuario role, THE Portal_Contratacion SHALL redirect to `/admin/planes` (default module)
3. THE login page SHALL display the title "Administrador de CRM" instead of "Panel de Administración"

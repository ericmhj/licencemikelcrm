import { Routes } from '@angular/router';
import { roleGuard } from '../../core/guards/role.guard';
import { platformAdminGuard } from '../../core/guards/platform-admin.guard';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./layout/admin-layout.component').then(c => c.AdminLayoutComponent),
    children: [
      {
        path: 'planes',
        loadComponent: () =>
          import('./plans/plan-list.component').then(c => c.PlanListComponent),
      },
      {
        path: 'planes/nuevo',
        loadComponent: () =>
          import('./plans/plan-form.component').then(c => c.PlanFormComponent),
      },
      {
        path: 'planes/editar/:codigo',
        loadComponent: () =>
          import('./plans/plan-form.component').then(c => c.PlanFormComponent),
      },
      {
        path: 'funcion_roles',
        loadComponent: () =>
          import('./funcion-roles/funcion-roles-list.component').then(c => c.FuncionRolesListComponent),
      },
      {
        path: 'funcion_roles/nuevo',
        loadComponent: () =>
          import('./funcion-roles/funcion-roles-form.component').then(c => c.FuncionRolesFormComponent),
      },
      {
        path: 'funcion_roles/editar/:codigo',
        loadComponent: () =>
          import('./funcion-roles/funcion-roles-form.component').then(c => c.FuncionRolesFormComponent),
      },
      {
        path: 'cartera',
        canActivate: [platformAdminGuard],
        loadComponent: () =>
          import('./cartera/cartera-list.component').then(c => c.CarteraListComponent),
      },
      {
        path: 'cartera/:tenantId',
        canActivate: [platformAdminGuard],
        loadComponent: () =>
          import('./cartera/cartera-detail.component').then(c => c.CarteraDetailComponent),
      },
      {
        path: 'estados-cuenta',
        canActivate: [platformAdminGuard],
        loadComponent: () =>
          import('./estados-cuenta/estado-cuenta-list.component').then(c => c.EstadoCuentaListComponent),
      },
      {
        path: 'estados-cuenta/:tenantId',
        canActivate: [platformAdminGuard],
        loadComponent: () =>
          import('./estados-cuenta/estado-cuenta-detail.component').then(c => c.EstadoCuentaDetailComponent),
      },
      { path: '', redirectTo: 'planes', pathMatch: 'full' },
    ],
  },
];

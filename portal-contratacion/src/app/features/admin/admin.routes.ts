import { Routes } from '@angular/router';

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
        path: 'form_templates',
        loadComponent: () =>
          import('./form-templates/form-template-list.component').then(c => c.FormTemplateListComponent),
      },
      {
        path: 'form_templates/nuevo',
        loadComponent: () =>
          import('./form-templates/form-template-form.component').then(c => c.FormTemplateFormComponent),
      },
      {
        path: 'form_templates/editar/:id',
        loadComponent: () =>
          import('./form-templates/form-template-form.component').then(c => c.FormTemplateFormComponent),
      },
      { path: '', redirectTo: 'planes', pathMatch: 'full' },
    ],
  },
];

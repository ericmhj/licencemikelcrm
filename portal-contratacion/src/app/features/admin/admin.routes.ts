import { Routes } from '@angular/router';

export const ADMIN_ROUTES: Routes = [
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
  { path: '', redirectTo: 'planes', pathMatch: 'full' },
];

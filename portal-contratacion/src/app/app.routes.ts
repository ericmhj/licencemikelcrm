import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  {
    path: 'contratacion',
    loadChildren: () =>
      import('./features/public-portal/public-portal.routes')
        .then(m => m.PUBLIC_PORTAL_ROUTES),
  },
  {
    path: 'dashboard',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN_CUENTA', 'SUPERVISOR'] },
    loadChildren: () =>
      import('./features/dashboard/dashboard.routes')
        .then(m => m.DASHBOARD_ROUTES),
  },
  { path: '', redirectTo: 'contratacion', pathMatch: 'full' },
  {
    path: 'acceso-denegado',
    loadComponent: () =>
      import('./shared/components/access-denied/access-denied.component')
        .then(c => c.AccessDeniedComponent),
  },
  { path: '**', redirectTo: 'contratacion' },
];

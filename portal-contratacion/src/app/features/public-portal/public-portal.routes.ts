import { Routes } from '@angular/router';
import { PublicLayoutComponent } from './layout/public-layout.component';

export const PUBLIC_PORTAL_ROUTES: Routes = [
  { path: '', component: PublicLayoutComponent },
  {
    path: 'solicitud',
    loadComponent: () =>
      import('./components/contract-form/contract-form.component')
        .then(c => c.ContractFormComponent),
  },
  {
    path: 'confirmacion',
    loadComponent: () =>
      import('./components/contract-confirmation/contract-confirmation.component')
        .then(c => c.ContractConfirmationComponent),
  },
];

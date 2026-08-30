import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * Guard que restringe acceso exclusivamente a platform_admin.
 * Si el usuario no tiene ese rol, redirige a /not-found (simula que la ruta no existe)
 * para no revelar la existencia del recurso protegido.
 */
export const platformAdminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.getUserRole() === 'platform_admin') {
    return true;
  }

  // Redirigir a not-found — no revelar que la ruta existe
  return router.createUrlTree(['/not-found']);
};

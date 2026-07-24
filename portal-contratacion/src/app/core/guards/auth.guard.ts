import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Verify token exists AND is not expired
  const token = authService.getToken();
  if (!token) {
    authService.logout();
    return router.createUrlTree(['/login']);
  }

  // Decode and check expiration
  try {
    const parts = token.split('.');
    if (parts.length !== 3) {
      authService.logout();
      return router.createUrlTree(['/login']);
    }
    const payload = JSON.parse(atob(parts[1]));
    if (!payload.exp || payload.exp * 1000 < Date.now()) {
      authService.logout();
      return router.createUrlTree(['/login']);
    }
  } catch {
    authService.logout();
    return router.createUrlTree(['/login']);
  }

  if (authService.isAuthenticated()) {
    return true;
  }

  authService.logout();
  return router.createUrlTree(['/login']);
};

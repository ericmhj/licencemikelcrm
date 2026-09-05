import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const authService = inject(AuthService);

  // Flujo público de onboarding (sin sesión): no redirigir a login/acceso-denegado.
  // Si HAY sesión activa (p.ej. admin operando), sí aplicamos el manejo aunque
  // el endpoint sea de contratación, porque el 401 aquí significa que el refresh
  // (authInterceptor) ya falló.
  const publicPaths = ['/api/v1/tenants', '/api/v1/payments'];
  const isOnboardingEndpoint = publicPaths.some(path => req.url.includes(path));
  const hasSession = authService.isAuthenticated();
  const skipHandling = isOnboardingEndpoint && !hasSession;

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (!skipHandling) {
        if (error.status === 401) {
          authService.logout();
          router.navigate(['/login']);
        }
        if (error.status === 403) {
          router.navigate(['/acceso-denegado']);
        }
      }
      return throwError(() => error);
    })
  );
};

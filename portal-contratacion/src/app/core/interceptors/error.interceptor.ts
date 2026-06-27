import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const authService = inject(AuthService);

  // Don't intercept public endpoints (onboarding flow)
  const publicPaths = ['/api/v1/tenants', '/api/v1/payments'];
  const isPublicRequest = publicPaths.some(path => req.url.includes(path));

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (!isPublicRequest) {
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

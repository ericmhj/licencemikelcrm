import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const authService = inject(AuthService);

  // Don't intercept public endpoints (onboarding flow) or SMT Backend calls
  const publicPaths = ['/api/v1/tenants', '/api/v1/payments'];
  const isPublicRequest = publicPaths.some(path => req.url.includes(path));
  const isSmtRequest = req.url.includes('localhost:3001') || req.url.includes('/api/form-templates');

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (!isPublicRequest && !isSmtRequest) {
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

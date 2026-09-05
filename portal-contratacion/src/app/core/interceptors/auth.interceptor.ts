import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Adjunta el access token a las peticiones /api/ y, ante un 401 (token expirado),
 * intenta renovar el token con el refresh_token una sola vez y reintenta la
 * petición original. Si el refresh falla, propaga el error.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  const isApi = req.url.includes('/api/');
  // No interceptar la propia renovación de token (Keycloak token endpoint).
  const isTokenEndpoint = req.url.includes('/protocol/openid-connect/token');

  if (!isApi || isTokenEndpoint) {
    return next(req);
  }

  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // Solo intentamos refresh si hubo 401 y tenemos refresh token disponible.
      if (error.status === 401 && authService.getRefreshToken()) {
        return authService.refreshToken().pipe(
          switchMap((newToken) => {
            if (!newToken) {
              return throwError(() => error);
            }
            const retried = req.clone({
              setHeaders: { Authorization: `Bearer ${newToken}` },
            });
            return next(retried);
          }),
        );
      }
      return throwError(() => error);
    }),
  );
};

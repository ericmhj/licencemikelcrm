import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, from, throwError } from 'rxjs';
import { JwtPayload, UserRole, AuthState } from '../models/auth.model';
import { environment } from '../../../environments/environment';

interface TokenResponse {
  access_token: string;
  refresh_token?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly TOKEN_KEY = 'auth_token';
  private readonly REFRESH_KEY = 'auth_refresh_token';

  private http = inject(HttpClient);

  private _authState = signal<AuthState>({
    isAuthenticated: false,
    token: null,
    user: null,
    tenantId: null,
  });

  /** Refresh en curso (para deduplicar peticiones concurrentes que refrescan). */
  private refreshInFlight: Promise<string | null> | null = null;

  readonly isAuthenticated = computed(() => this._authState().isAuthenticated);
  readonly userRole = computed(() => this._authState().user?.rol ?? null);
  readonly tenantId = computed(() => this._authState().tenantId);

  constructor() {
    this.initFromStorage();
  }

  getToken(): string | null {
    return this._authState().token;
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_KEY);
  }

  getUserRole(): UserRole | null {
    const user = this._authState().user;
    if (!user) return null;
    // Support 'rol' (License Service JWT)
    if (user.rol) return user.rol;

    // Collect roles from all possible JWT structures
    let allRoles: string[] = [];

    // Direct 'roles' array (some JWT configurations)
    if (user.roles && user.roles.length > 0) {
      allRoles = [...allRoles, ...user.roles];
    }

    // Keycloak standard: realm_access.roles
    if (user.realm_access?.roles && user.realm_access.roles.length > 0) {
      allRoles = [...allRoles, ...user.realm_access.roles];
    }

    // Filter to known application roles, return highest priority match
    const APP_ROLES: UserRole[] = ['platform_admin', 'superusuario', 'admin', 'manager', 'tecnico', 'asistente'];
    const appRole = APP_ROLES.find(r => allRoles.includes(r));
    return appRole ?? null;
  }

  getTenantId(): string | null {
    const user = this._authState().user;
    if (!user) return null;
    return user.tenantId ?? user.tenant_id ?? null;
  }

  login(token: string, refreshToken?: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
    if (refreshToken) {
      localStorage.setItem(this.REFRESH_KEY, refreshToken);
    }
    const payload = this.decodeToken(token);
    this._authState.set({
      isAuthenticated: true,
      token,
      user: payload,
      tenantId: payload?.tenantId ?? null,
    });
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_KEY);
    this._authState.set({
      isAuthenticated: false,
      token: null,
      user: null,
      tenantId: null,
    });
  }

  /**
   * Renueva el access token usando el refresh_token contra Keycloak
   * (grant_type=refresh_token). Devuelve el nuevo access token o null si falla.
   *
   * Deduplica llamadas concurrentes: si ya hay un refresh en curso, todas las
   * peticiones esperan el mismo resultado.
   */
  refreshToken(): Observable<string | null> {
    if (!this.refreshInFlight) {
      this.refreshInFlight = this.performRefresh().finally(() => {
        this.refreshInFlight = null;
      });
    }
    return from(this.refreshInFlight);
  }

  private async performRefresh(): Promise<string | null> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      return null;
    }

    const body = new URLSearchParams();
    body.set('grant_type', 'refresh_token');
    body.set('client_id', environment.keycloak.clientId);
    body.set('client_secret', environment.keycloak.clientSecret);
    body.set('refresh_token', refreshToken);

    const headers = new HttpHeaders({
      'Content-Type': 'application/x-www-form-urlencoded',
    });

    try {
      const response = await this.http
        .post<TokenResponse>(environment.keycloak.tokenUrl, body.toString(), { headers })
        .toPromise();

      if (response?.access_token) {
        this.login(response.access_token, response.refresh_token ?? refreshToken);
        return response.access_token;
      }
      return null;
    } catch {
      // Refresh inválido/expirado: limpiar sesión.
      this.logout();
      return null;
    }
  }

  private initFromStorage(): void {
    const token = localStorage.getItem(this.TOKEN_KEY);
    if (token) {
      const payload = this.decodeToken(token);
      if (payload && payload.exp * 1000 > Date.now()) {
        this._authState.set({
          isAuthenticated: true,
          token,
          user: payload,
          tenantId: payload.tenantId ?? payload.tenant_id ?? null,
        });
      } else {
        localStorage.removeItem(this.TOKEN_KEY);
      }
    }
  }

  private decodeToken(token: string): JwtPayload | null {
    try {
      const parts = token.split('.');
      if (parts.length !== 3) return null;
      const payload = JSON.parse(atob(parts[1]));
      return payload as JwtPayload;
    } catch {
      return null;
    }
  }
}

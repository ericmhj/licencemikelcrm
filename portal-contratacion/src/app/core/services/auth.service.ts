import { Injectable, signal, computed } from '@angular/core';
import { JwtPayload, UserRole, AuthState } from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly TOKEN_KEY = 'auth_token';

  private _authState = signal<AuthState>({
    isAuthenticated: false,
    token: null,
    user: null,
    tenantId: null,
  });

  readonly isAuthenticated = computed(() => this._authState().isAuthenticated);
  readonly userRole = computed(() => this._authState().user?.rol ?? null);
  readonly tenantId = computed(() => this._authState().tenantId);

  constructor() {
    this.initFromStorage();
  }

  getToken(): string | null {
    return this._authState().token;
  }

  getUserRole(): UserRole | null {
    return this._authState().user?.rol ?? null;
  }

  getTenantId(): string | null {
    return this._authState().tenantId;
  }

  login(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
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
    this._authState.set({
      isAuthenticated: false,
      token: null,
      user: null,
      tenantId: null,
    });
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
          tenantId: payload.tenantId,
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

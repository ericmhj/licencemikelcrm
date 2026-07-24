export interface JwtPayload {
  sub: string;
  tenantId?: string;
  tenant_id?: string;
  rol?: UserRole;
  roles?: string[];
  realm_access?: { roles: string[] };
  resource_access?: Record<string, { roles: string[] }>;
  exp: number;
  iat: number;
}

export type UserRole = 'platform_admin' | 'superusuario' | 'admin' | 'manager' | 'tecnico' | 'asistente';

export interface AuthState {
  isAuthenticated: boolean;
  token: string | null;
  user: JwtPayload | null;
  tenantId: string | null;
}

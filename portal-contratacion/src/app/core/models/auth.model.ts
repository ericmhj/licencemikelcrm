export interface JwtPayload {
  sub: string;
  tenantId: string;
  rol: UserRole;
  exp: number;
  iat: number;
}

export type UserRole = 'ADMIN_CUENTA' | 'SUPERVISOR' | 'TECNICO' | 'ASISTENTE';

export interface AuthState {
  isAuthenticated: boolean;
  token: string | null;
  user: JwtPayload | null;
  tenantId: string | null;
}

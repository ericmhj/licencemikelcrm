export interface FuncionRoles {
  id: string;
  codigo: string;
  descripcion: string;
  funcionalidad: string;
  roles: string[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateFuncionRolesRequest {
  codigo: string;
  descripcion: string;
  funcionalidad: string;
  roles: string[];
}

export interface UpdateFuncionRolesRequest {
  descripcion: string;
  funcionalidad: string;
  roles: string[];
}

export const VALID_ROLES: string[] = [
  'tecnico',
  'asistente',
  'manager',
  'admin',
  'superusuario'
];

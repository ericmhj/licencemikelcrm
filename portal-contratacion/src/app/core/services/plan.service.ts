import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Plan {
  id: string;
  codigo: string;
  nombre: string;
  descripcion: string;
  creditosMensuales: number;
  maxFreeDownloads: number;
  maxUsuarios: number;
  rolesAutorizados: string[];
  precioMensual: number;
  activo: boolean;
}

export interface PlanLimits {
  codigo: string;
  creditos_mensuales: number;
  max_free_downloads: number;
  max_usuarios: number;
  roles_autorizados: string[];
}

export interface CreatePlanRequest {
  codigo: string;
  nombre: string;
  descripcion: string;
  creditosMensuales: number;
  maxFreeDownloads: number;
  maxUsuarios: number;
  rolesAutorizados: string[];
  precioMensual: number;
}

@Injectable({ providedIn: 'root' })
export class PlanService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/plans`;

  getAll(): Observable<Plan[]> {
    return this.http.get<Plan[]>(this.baseUrl);
  }

  getByCode(codigo: string): Observable<Plan> {
    return this.http.get<Plan>(`${this.baseUrl}/${codigo}`);
  }

  getLimits(codigo: string): Observable<PlanLimits> {
    return this.http.get<PlanLimits>(`${this.baseUrl}/${codigo}/limits`);
  }

  create(plan: CreatePlanRequest): Observable<Plan> {
    return this.http.post<Plan>(this.baseUrl, plan);
  }

  update(codigo: string, data: Partial<CreatePlanRequest>): Observable<Plan> {
    return this.http.put<Plan>(`${this.baseUrl}/${codigo}`, data);
  }

  deactivate(codigo: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${codigo}`);
  }
}

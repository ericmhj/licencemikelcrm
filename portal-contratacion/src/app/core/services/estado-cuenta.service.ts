import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, timeout } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PaginatedEdoCuenta, TenantEdoCuentaResumen } from '../models/estado-cuenta.model';
import { TenantSummary } from '../models/cartera.model';

@Injectable({ providedIn: 'root' })
export class EstadoCuentaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1`;
  private readonly TIMEOUT = 15_000;

  /**
   * List all tenants with their estado de cuenta summary.
   */
  listTenants(): Observable<TenantSummary[]> {
    return this.http
      .get<TenantSummary[]>(`${this.baseUrl}/cartera/tenants`)
      .pipe(timeout(this.TIMEOUT));
  }

  /**
   * Get estado de cuenta summary for a specific tenant.
   */
  getResumen(tenantId: string): Observable<TenantEdoCuentaResumen> {
    return this.http
      .get<TenantEdoCuentaResumen>(`${this.baseUrl}/tenants/${tenantId}/estado-cuenta/resumen`)
      .pipe(timeout(this.TIMEOUT));
  }

  /**
   * Get paginated estado de cuenta movements for a tenant.
   */
  getMovimientos(
    tenantId: string,
    filters: { tipo?: string; desde?: string; hasta?: string; page?: number; pageSize?: number },
  ): Observable<PaginatedEdoCuenta> {
    let params = new HttpParams();
    if (filters.tipo) params = params.set('tipo', filters.tipo);
    if (filters.desde) params = params.set('desde', filters.desde);
    if (filters.hasta) params = params.set('hasta', filters.hasta);
    if (filters.page) params = params.set('page', String(filters.page));
    if (filters.pageSize) params = params.set('pageSize', String(filters.pageSize));

    return this.http
      .get<PaginatedEdoCuenta>(`${this.baseUrl}/tenants/${tenantId}/estado-cuenta`, { params })
      .pipe(timeout(this.TIMEOUT));
  }
}

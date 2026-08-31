import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, timeout, retry } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  TenantSummary,
  TenantBalance,
  PaginatedLedger,
  AdjustCreditRequest,
  AdjustCreditResponse,
} from '../models/cartera.model';

@Injectable({ providedIn: 'root' })
export class CarteraService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1`;
  private readonly TIMEOUT = 15_000;

  /**
   * List all tenants.
   * Only accessible by platform_admin.
   */
  listAllTenants(): Observable<TenantSummary[]> {
    return this.http
      .get<TenantSummary[]>(`${this.baseUrl}/cartera/tenants`)
      .pipe(timeout(this.TIMEOUT));
  }

  /**
   * Search tenants by name or slug.
   * Only accessible by platform_admin.
   */
  searchTenants(query: string): Observable<TenantSummary[]> {
    const params = new HttpParams().set('q', query);
    return this.http
      .get<TenantSummary[]>(`${this.baseUrl}/tenants/search`, { params })
      .pipe(timeout(this.TIMEOUT));
  }

  /**
   * Get the credit balance for a specific tenant.
   */
  getBalance(tenantId: string): Observable<TenantBalance> {
    return this.http
      .get<TenantBalance>(`${this.baseUrl}/tenants/${tenantId}/credits/balance`)
      .pipe(timeout(this.TIMEOUT));
  }

  /**
   * Get paginated ledger history for a tenant.
   */
  getHistory(
    tenantId: string,
    filters: { tipo?: string; desde?: string; hasta?: string; page?: number; pageSize?: number },
  ): Observable<PaginatedLedger> {
    let params = new HttpParams();
    if (filters.tipo) params = params.set('tipo', filters.tipo);
    if (filters.desde) params = params.set('desde', filters.desde);
    if (filters.hasta) params = params.set('hasta', filters.hasta);
    if (filters.page) params = params.set('page', String(filters.page));
    if (filters.pageSize) params = params.set('pageSize', String(filters.pageSize));

    return this.http
      .get<PaginatedLedger>(`${this.baseUrl}/tenants/${tenantId}/credits/history`, { params })
      .pipe(timeout(this.TIMEOUT));
  }

  /**
   * Adjust credit balance for a tenant (recarga or descuento).
   * Only accessible by platform_admin.
   * Requires operationId for idempotency.
   */
  adjustCredits(tenantId: string, request: AdjustCreditRequest): Observable<AdjustCreditResponse> {
    return this.http
      .post<AdjustCreditResponse>(
        `${this.baseUrl}/tenants/${tenantId}/credits/adjust`,
        request,
      )
      .pipe(timeout(this.TIMEOUT));
  }

  /**
   * Asigna/cambia el plan de un tenant. Solo platform_admin.
   * El license-service actualiza tenant.plan_id y propaga el cambio a SMT.
   */
  updateTenantPlan(tenantId: string, planId: string): Observable<{ id: string; correlationId: string }> {
    return this.http
      .patch<{ id: string; correlationId: string }>(
        `${this.baseUrl}/tenants/${tenantId}`,
        { planId },
      )
      .pipe(timeout(this.TIMEOUT));
  }
}

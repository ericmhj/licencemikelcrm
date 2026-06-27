import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, timeout, retry } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AccessResponse, ContractResponse, CreditResponse, ReactivationSummaryResponse } from '../models/api-responses.model';

@Injectable({ providedIn: 'root' })
export class LicenseApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1`;
  private readonly DEFAULT_TIMEOUT = 10_000;
  private readonly RETRY_COUNT = 1;

  getAccess(tenantId: string): Observable<AccessResponse> {
    return this.http.get<AccessResponse>(`${this.baseUrl}/access/${tenantId}`).pipe(
      timeout(this.DEFAULT_TIMEOUT),
      retry(this.RETRY_COUNT),
    );
  }

  getContracts(tenantId: string): Observable<ContractResponse> {
    return this.http.get<ContractResponse>(`${this.baseUrl}/tenants/${tenantId}/contracts`).pipe(
      timeout(this.DEFAULT_TIMEOUT),
      retry(this.RETRY_COUNT),
    );
  }

  getCredits(tenantId: string): Observable<CreditResponse> {
    return this.http.get<CreditResponse>(`${this.baseUrl}/tenants/${tenantId}/credits`).pipe(
      timeout(this.DEFAULT_TIMEOUT),
      retry(this.RETRY_COUNT),
    );
  }

  getReactivationSummary(tenantId: string): Observable<ReactivationSummaryResponse> {
    return this.http.get<ReactivationSummaryResponse>(`${this.baseUrl}/tenants/${tenantId}/reactivation-summary`).pipe(
      timeout(this.DEFAULT_TIMEOUT),
      retry(this.RETRY_COUNT),
    );
  }
}

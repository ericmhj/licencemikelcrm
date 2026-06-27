import { Injectable, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { TimeoutError } from 'rxjs';
import { LicenseApiService } from '../../../core/services/license-api.service';
import { AuthService } from '../../../core/services/auth.service';
import { ContractResponse, CreditResponse, AccessResponse } from '../../../core/models/api-responses.model';

@Injectable()
export class DashboardStateService {
  private readonly licenseApi = inject(LicenseApiService);
  private readonly authService = inject(AuthService);

  readonly contractLoading = signal(false);
  readonly creditsLoading = signal(false);

  readonly contractError = signal<string | null>(null);
  readonly creditsError = signal<string | null>(null);

  readonly contractData = signal<ContractResponse | null>(null);
  readonly creditData = signal<CreditResponse | null>(null);
  readonly accessData = signal<AccessResponse | null>(null);

  loadAllSections(): void {
    const tenantId = this.authService.getTenantId();
    if (!tenantId) return;

    this.loadContracts(tenantId);
    this.loadCredits(tenantId);
    this.loadAccess(tenantId);
  }

  retrySection(section: 'contracts' | 'credits'): void {
    const tenantId = this.authService.getTenantId();
    if (!tenantId) return;
    if (section === 'contracts') this.loadContracts(tenantId);
    if (section === 'credits') this.loadCredits(tenantId);
  }

  private loadContracts(tenantId: string): void {
    this.contractLoading.set(true);
    this.contractError.set(null);
    this.licenseApi.getContracts(tenantId).subscribe({
      next: (data) => {
        this.contractData.set(data);
        this.contractLoading.set(false);
      },
      error: (err) => {
        this.contractError.set(this.mapError(err));
        this.contractLoading.set(false);
      },
    });
  }

  private loadCredits(tenantId: string): void {
    this.creditsLoading.set(true);
    this.creditsError.set(null);
    this.licenseApi.getCredits(tenantId).subscribe({
      next: (data) => {
        this.creditData.set(data);
        this.creditsLoading.set(false);
      },
      error: (err) => {
        this.creditsError.set(this.mapError(err));
        this.creditsLoading.set(false);
      },
    });
  }

  private loadAccess(tenantId: string): void {
    this.licenseApi.getAccess(tenantId).subscribe({
      next: (data) => this.accessData.set(data),
      error: () => {},
    });
  }

  private mapError(err: unknown): string {
    if (err instanceof TimeoutError) return 'TIMEOUT';
    if (err instanceof HttpErrorResponse && err.status >= 500) return 'SERVER_ERROR';
    return 'UNKNOWN_ERROR';
  }
}

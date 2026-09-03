import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, timeout } from 'rxjs';
import { environment } from '../../../environments/environment';

export type TipoCobro = 'FIJO_MENSUAL' | 'VARIABLE_PREPAGO';

export interface ClabeTenant {
  id: string;
  clabe: string;
  tipo: TipoCobro;
  psp: string;
  activa: boolean;
  creadaEn: string | null;
}

export interface CreateClabeRequest {
  clabe: string;
  tipo: TipoCobro;
  psp?: string;
}

export interface PagoManualResponse {
  id: string | null;
  estado: string | null;
  monto: number;
  creditosOtorgados: number | null;
  claveRastreo: string;
}

@Injectable({ providedIn: 'root' })
export class CobroAdminService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/tenants`;
  private readonly TIMEOUT = 15_000;

  listClabes(tenantId: string): Observable<ClabeTenant[]> {
    return this.http
      .get<ClabeTenant[]>(`${this.baseUrl}/${tenantId}/clabes`)
      .pipe(timeout(this.TIMEOUT));
  }

  createClabe(tenantId: string, req: CreateClabeRequest): Observable<ClabeTenant> {
    return this.http
      .post<ClabeTenant>(`${this.baseUrl}/${tenantId}/clabes`, req)
      .pipe(timeout(this.TIMEOUT));
  }

  deactivateClabe(tenantId: string, clabeId: string): Observable<void> {
    return this.http
      .delete<void>(`${this.baseUrl}/${tenantId}/clabes/${clabeId}`)
      .pipe(timeout(this.TIMEOUT));
  }

  registrarPagoMensual(
    tenantId: string,
    monto: number,
    referencia?: string,
    claveRastreo?: string,
  ): Observable<PagoManualResponse> {
    return this.http
      .post<PagoManualResponse>(`${this.baseUrl}/${tenantId}/pagos/mensual`, {
        monto,
        referencia,
        claveRastreo,
      })
      .pipe(timeout(this.TIMEOUT));
  }
}

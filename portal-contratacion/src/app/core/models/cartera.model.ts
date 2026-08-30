/**
 * Models for the tenant consumption/wallet (cartera) administration module.
 */

export interface TenantSummary {
  id: string;
  slug: string;
  nombre: string;
  plan: string;
  estado: string;
}

export interface TenantBalance {
  tenantId: string;
  slug: string;
  nombre: string;
  plan: string;
  estado: string;
  saldoCreditos: number;
  creditosTotalesAdquiridos: number;
  creditosConsumidos: number;
  ultimoSync: string | null;
}

export interface LedgerEntry {
  id: string;
  tipo: 'consumo' | 'recarga' | 'bonus' | 'ajuste' | 'compensacion' | 'excedente';
  cantidad: number;
  saldoResultante: number;
  concepto: string;
  perfilDocumento: string | null;
  referencia: string | null;
  actorId: string | null;
  actorEmail: string | null;
  createdAt: string;
}

export interface PaginatedLedger {
  data: LedgerEntry[];
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

export interface AdjustCreditRequest {
  operationId: string;
  tipo: 'recarga' | 'ajuste';
  cantidad: number;
  motivo: string;
  referencia: string;
  autorizacion: string;
}

export interface AdjustCreditResponse {
  status: 'approved' | 'rejected';
  saldoResultante: number;
  eventoId: string;
  timestamp: string;
  message?: string;
}

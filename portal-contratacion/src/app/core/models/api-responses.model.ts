export interface AccessResponse {
  tenantId: string;
  status: 'ACTIVE' | 'SUSPENDED' | 'CANCELLED' | 'ONBOARDING';
  modules: string[];
  creditBalance: number;
  userRole: 'platform_admin' | 'superusuario' | 'admin' | 'manager' | 'tecnico' | 'asistente';
  cachedAt: string;
}

export interface ContractResponse {
  contracts: ContractDetail[];
}

export interface ContractDetail {
  contratoId: string;
  tipo: 'MODULO' | 'CREDITOS';
  modulo: string | null;
  estado: 'ACTIVE' | 'SUSPENDED' | 'CANCELLED' | 'EXPIRED';
  cuotaMensual: number;
  cuotasPagadas: number;
  cuotasTotales: number;
  proximaFechaCobro: string;
  fechaVencimientoContrato: string;
  renovacionAuto: boolean;
  descuentoDisponibleHoy: number;
  montoConDescuentoHoy: number;
}

export interface CreditResponse {
  saldoDisponible: number;
  creditosTotalesAdquiridos: number;
  umbralConsultasIncluidas: number;
  contadorGlobalConsultas: number;
  excedente: number;
  paqueteActivo: PaqueteActivo | null;
  alertaNivel: string | null;
}

export interface PaqueteActivo {
  paqueteId: string;
  creditosPaquete: number;
  creditosBonus: number;
  fechaVencimiento: string;
}

export interface ReactivationSummaryResponse {
  tenantId: string;
  diasEnMora: number;
  cuotasVencidas: CuotaVencidaDetail[];
  cuotasAlmacenamiento: CuotaAlmacenamientoDetail[];
  totalCuotasVencidas: number;
  totalAlmacenamiento: number;
  totalParaReactivar: number;
}

export interface CuotaVencidaDetail {
  cuotaId: string;
  periodo: string;
  monto: number;
  fechaLimite: string;
}

export interface CuotaAlmacenamientoDetail {
  periodo: string;
  reportes: number;
  monto: number;
}

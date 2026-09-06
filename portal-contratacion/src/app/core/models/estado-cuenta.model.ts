export interface MovimientoEdoCuenta {
  id: string;
  tipo: 'ABONO' | 'CARGO';
  monto: number;
  saldoResultante: number;
  concepto: string;
  referencia: string | null;
  claveRastreo: string | null;
  periodoMes: string | null;
  registradoEn: string;
}

export interface PaginatedEdoCuenta {
  data: MovimientoEdoCuenta[];
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

export interface TenantEdoCuentaResumen {
  tenantId: string;
  nombre: string;
  plan: string;
  estado: string;
  saldoActual: number;
  totalAbonos: number;
  totalCargos: number;
  // Total de pagos de renta (PAGO_RENTA), separado de los cargos variables.
  totalPagosRenta: number;
  // Adeudo de mensualidad (calculado, no es un movimiento del estado de cuenta)
  mensualidad: number;
  mesPagado: boolean;
  adeudoMes: number;
  periodoMes: string;
  servicioPagadoHasta: string | null;
}

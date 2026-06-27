export interface ModuloDisponible {
  id: string;
  nombre: string;
  descripcion: string;
  precioMensual: number;
  obligatorio: boolean;
}

export interface PaqueteCreditos {
  id: string;
  nombre: string;
  creditos: number;
  bonus: number;
  precioAnual: number;
}

export interface SubtotalItem {
  moduloId: string;
  nombre: string;
  precioMensual: number;
  subtotalAnual: number;
}

export interface CalculadoraState {
  modulosSeleccionados: ModuloDisponible[];
  paqueteSeleccionado: PaqueteCreditos | null;
  subtotales: SubtotalItem[];
  totalAnual: number;
  cuotaMensual: number;
}

export interface ResumenServicios {
  modulos: SubtotalItem[];
  paqueteCreditos: PaqueteCreditos | null;
  totalAnual: number;
  cuotaMensual: number;
  politicaDescuentos: string;
  fechaGeneracion: Date;
}

// Static data
export const MODULOS_DISPONIBLES: ModuloDisponible[] = [
  { id: 'CRM_BASE', nombre: 'CRM Base', descripcion: 'Gestión de clientes y contactos', precioMensual: 200, obligatorio: true },
  { id: 'REPORTES', nombre: 'Reportes', descripcion: 'Generación de informes técnicos', precioMensual: 150, obligatorio: false },
  { id: 'TICKETS', nombre: 'Tickets', descripcion: 'Sistema de soporte y seguimiento', precioMensual: 100, obligatorio: false },
  { id: 'ADMIN', nombre: 'Administrador', descripcion: 'Panel de administración avanzada', precioMensual: 250, obligatorio: false },
];

export const PAQUETES_CREDITOS: PaqueteCreditos[] = [
  { id: 'PKG_100', nombre: 'Paquete 100', creditos: 100, bonus: 4, precioAnual: 500 },
  { id: 'PKG_500', nombre: 'Paquete 500', creditos: 500, bonus: 4, precioAnual: 2000 },
];

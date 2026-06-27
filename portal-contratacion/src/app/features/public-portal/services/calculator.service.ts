import { Injectable } from '@angular/core';
import { ModuloDisponible, PaqueteCreditos, SubtotalItem } from '../../../core/models/calculator.model';

@Injectable({ providedIn: 'root' })
export class CalculatorService {

  calcularSubtotales(modules: ModuloDisponible[]): SubtotalItem[] {
    return modules.map(m => ({
      moduloId: m.id,
      nombre: m.nombre,
      precioMensual: m.precioMensual,
      subtotalAnual: m.precioMensual * 12,
    }));
  }

  calcularTotalAnual(modules: ModuloDisponible[], paquete: PaqueteCreditos | null): number {
    const totalModulos = modules.reduce((sum, m) => sum + m.precioMensual * 12, 0);
    return totalModulos + (paquete?.precioAnual ?? 0);
  }

  calcularCuotaMensual(totalAnual: number): number {
    return totalAnual / 12;
  }

  calcularDescuento(monto: number, fechaPago: Date, fechaLimite: Date): { porcentaje: number; montoFinal: number } {
    const pago = new Date(fechaPago.getFullYear(), fechaPago.getMonth(), fechaPago.getDate()).getTime();
    const limite = new Date(fechaLimite.getFullYear(), fechaLimite.getMonth(), fechaLimite.getDate()).getTime();

    if (pago < limite) {
      return { porcentaje: 10, montoFinal: monto * 0.90 };
    }
    if (pago === limite) {
      return { porcentaje: 3, montoFinal: monto * 0.97 };
    }
    return { porcentaje: 0, montoFinal: monto };
  }
}

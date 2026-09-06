import { Injectable, signal, computed } from '@angular/core';
import { PaqueteCreditos, SubtotalItem } from '../../../core/models/calculator.model';
import { Plan } from '../../../core/services/plan.service';

/**
 * Estado de la calculadora del portal de contratación.
 *
 * Modelo por PLAN (selección única): el usuario elige un plan del catálogo
 * (los mismos de /admin/planes) y la calculadora recalcula con su precio
 * mensual. Opcionalmente se agrega un paquete de créditos.
 */
@Injectable()
export class CalculatorStateService {
  private readonly _selectedPlan = signal<Plan | null>(null);
  private readonly _selectedPackage = signal<PaqueteCreditos | null>(null);

  readonly selectedPlan = this._selectedPlan.asReadonly();
  readonly selectedPackage = this._selectedPackage.asReadonly();

  /** Desglose para la calculadora: el plan como única línea de servicio. */
  readonly subtotales = computed<SubtotalItem[]>(() => {
    const plan = this._selectedPlan();
    if (!plan) return [];
    return [{
      moduloId: plan.codigo,
      nombre: plan.nombre,
      precioMensual: plan.precioMensual,
      subtotalAnual: plan.precioMensual * 12,
    }];
  });

  readonly totalAnual = computed<number>(() => {
    const plan = this._selectedPlan();
    const planAnual = plan ? plan.precioMensual * 12 : 0;
    return planAnual + (this._selectedPackage()?.precioAnual ?? 0);
  });

  readonly cuotaMensual = computed<number>(() => this.totalAnual() / 12);

  selectPlan(plan: Plan): void {
    this._selectedPlan.set(plan);
  }

  selectPackage(pkg: PaqueteCreditos | null): void {
    this._selectedPackage.set(pkg);
  }

  reset(): void {
    this._selectedPlan.set(null);
    this._selectedPackage.set(null);
  }
}

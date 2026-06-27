import { Injectable, inject, signal, computed } from '@angular/core';
import { CalculatorService } from './calculator.service';
import { ModuloDisponible, PaqueteCreditos, MODULOS_DISPONIBLES } from '../../../core/models/calculator.model';

@Injectable()
export class CalculatorStateService {
  private readonly calculatorService = inject(CalculatorService);

  private readonly _selectedModules = signal<ModuloDisponible[]>(
    MODULOS_DISPONIBLES.filter(m => m.obligatorio)
  );
  private readonly _selectedPackage = signal<PaqueteCreditos | null>(null);

  readonly selectedModules = this._selectedModules.asReadonly();
  readonly selectedPackage = this._selectedPackage.asReadonly();

  readonly subtotales = computed(() =>
    this.calculatorService.calcularSubtotales(this._selectedModules())
  );
  readonly totalAnual = computed(() =>
    this.calculatorService.calcularTotalAnual(this._selectedModules(), this._selectedPackage())
  );
  readonly cuotaMensual = computed(() =>
    this.calculatorService.calcularCuotaMensual(this.totalAnual())
  );

  toggleModule(module: ModuloDisponible): void {
    if (module.obligatorio) return;
    this._selectedModules.update(modules => {
      const exists = modules.find(m => m.id === module.id);
      return exists
        ? modules.filter(m => m.id !== module.id)
        : [...modules, module];
    });
  }

  selectPackage(pkg: PaqueteCreditos | null): void {
    this._selectedPackage.set(pkg);
  }

  reset(): void {
    this._selectedModules.set(MODULOS_DISPONIBLES.filter(m => m.obligatorio));
    this._selectedPackage.set(null);
  }
}

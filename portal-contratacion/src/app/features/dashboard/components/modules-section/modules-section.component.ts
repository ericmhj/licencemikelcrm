import { Component, computed, inject, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { ContractResponse, AccessResponse } from '../../../../core/models/api-responses.model';
import { AuthService } from '../../../../core/services/auth.service';
import { MODULOS_DISPONIBLES } from '../../../../core/models/calculator.model';
import { CurrencyEurPipe } from '../../../../shared/pipes/currency-eur.pipe';

@Component({
  selector: 'app-modules-section',
  standalone: true,
  imports: [CommonModule, MatButtonModule, CurrencyEurPipe],
  template: `
    <div class="bg-white rounded-lg shadow p-6">
      <h2 class="text-lg font-semibold mb-4">Módulos</h2>
      <div class="space-y-3">
        @for (mod of activeModules(); track mod.contratoId) {
          <div class="flex items-center justify-between py-2 border-b last:border-b-0">
            <div class="flex items-center gap-2">
              <span class="w-2 h-2 rounded-full bg-success"></span>
              <span class="font-medium">{{ mod.modulo }}</span>
            </div>
            <div class="text-sm text-gray-600 flex gap-4">
              <span>{{ mod.cuotaMensual | currencyEur }}</span>
              <span>{{ formatDate(mod.proximaFechaCobro) }}</span>
            </div>
          </div>
        }
        @for (mod of inactiveModules(); track mod.id) {
          <div class="flex items-center justify-between py-2 border-b last:border-b-0 text-muted">
            <div class="flex items-center gap-2">
              <span class="w-2 h-2 rounded-full bg-gray-300"></span>
              <span>{{ mod.nombre }}</span>
            </div>
            @if (isAdmin()) {
              <button mat-stroked-button color="primary" class="text-xs min-h-[44px]">
                + Añadir módulo
              </button>
            }
          </div>
        }
      </div>
    </div>
  `,
})
export class ModulesSectionComponent {
  private readonly authService = inject(AuthService);

  contractData = input.required<ContractResponse | null>();
  accessData = input<AccessResponse | null>(null);

  readonly isAdmin = computed(() => this.authService.getUserRole() === 'admin');

  activeModules = computed(() => {
    const data = this.contractData();
    if (!data) return [];
    return data.contracts.filter(c => c.estado === 'ACTIVE' && c.modulo);
  });

  inactiveModules = computed(() => {
    const data = this.contractData();
    const activeModuleNames = this.activeModules().map(m => m.modulo);
    return MODULOS_DISPONIBLES.filter(m => !activeModuleNames.includes(m.nombre));
  });

  formatDate(isoDate: string): string {
    const date = new Date(isoDate);
    return date.toLocaleDateString('es-ES', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }
}

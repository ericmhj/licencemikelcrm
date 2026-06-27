import { Component, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ContractResponse } from '../../../../core/models/api-responses.model';
import { CurrencyEurPipe } from '../../../../shared/pipes/currency-eur.pipe';

@Component({
  selector: 'app-quota-section',
  standalone: true,
  imports: [CommonModule, CurrencyEurPipe],
  template: `
    <div class="bg-white rounded-lg shadow p-6">
      <h2 class="text-lg font-semibold mb-4">Próxima Cuota</h2>
      @if (primaryContract(); as contract) {
        <dl class="space-y-2">
          <div class="flex justify-between">
            <dt class="text-gray-500">Monto</dt>
            <dd class="font-medium text-lg">{{ contract.cuotaMensual | currencyEur }}</dd>
          </div>
          <div class="flex justify-between">
            <dt class="text-gray-500">Fecha límite</dt>
            <dd>{{ formatDate(contract.proximaFechaCobro) }}</dd>
          </div>
          <div class="flex justify-between items-center">
            <dt class="text-gray-500">Descuento hoy</dt>
            <dd class="font-medium" [class]="descuentoClass()">{{ descuentoHoy() }}</dd>
          </div>
        </dl>
      } @else {
        <p class="text-gray-500">Sin información de cuota.</p>
      }
    </div>
  `,
})
export class QuotaSectionComponent {
  contractData = input.required<ContractResponse | null>();

  primaryContract = computed(() => {
    const data = this.contractData();
    if (!data || data.contracts.length === 0) return null;
    return data.contracts[0];
  });

  descuentoHoy = computed(() => {
    const contract = this.primaryContract();
    if (!contract) return 'Sin descuento';
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    const fechaCobro = new Date(contract.proximaFechaCobro);
    fechaCobro.setHours(0, 0, 0, 0);

    if (hoy.getTime() < fechaCobro.getTime()) {
      return '10% disponible';
    }
    if (hoy.getTime() === fechaCobro.getTime()) {
      return '3% disponible';
    }
    return 'Sin descuento';
  });

  descuentoClass = computed(() => {
    const desc = this.descuentoHoy();
    if (desc.includes('10%')) return 'text-success';
    if (desc.includes('3%')) return 'text-warning';
    return 'text-danger';
  });

  formatDate(isoDate: string): string {
    const date = new Date(isoDate);
    return date.toLocaleDateString('es-ES', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }
}

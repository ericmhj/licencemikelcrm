import { Component, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ContractResponse } from '../../../../core/models/api-responses.model';

@Component({
  selector: 'app-contract-section',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="bg-white rounded-lg shadow p-6">
      <h2 class="text-lg font-semibold mb-4">Contrato</h2>
      @if (primaryContract(); as contract) {
        <dl class="space-y-2">
          <div class="flex justify-between">
            <dt class="text-gray-500">Código</dt>
            <dd class="font-medium">{{ contract.contratoId }}</dd>
          </div>
          <div class="flex justify-between">
            <dt class="text-gray-500">Fecha inicio</dt>
            <dd>{{ formatDate(contract.proximaFechaCobro) }}</dd>
          </div>
          <div class="flex justify-between">
            <dt class="text-gray-500">Fecha vencimiento</dt>
            <dd>{{ formatDate(contract.fechaVencimientoContrato) }}</dd>
          </div>
          <div class="flex justify-between">
            <dt class="text-gray-500">Días restantes</dt>
            <dd class="font-medium">{{ diasRestantes() }}</dd>
          </div>
          <div class="flex justify-between items-center">
            <dt class="text-gray-500">Estado</dt>
            <dd>
              <span class="px-2 py-1 rounded-full text-xs font-medium" [class]="estadoClass()">
                {{ estadoLabel() }}
              </span>
            </dd>
          </div>
        </dl>
      } @else {
        <p class="text-gray-500">No hay contrato disponible.</p>
      }
    </div>
  `,
})
export class ContractSectionComponent {
  contractData = input.required<ContractResponse | null>();
  loading = input<boolean>(false);
  error = input<string | null>(null);

  primaryContract = computed(() => {
    const data = this.contractData();
    if (!data || data.contracts.length === 0) return null;
    return data.contracts[0];
  });

  diasRestantes = computed(() => {
    const contract = this.primaryContract();
    if (!contract) return 0;
    const vencimiento = new Date(contract.fechaVencimientoContrato);
    const hoy = new Date();
    const diff = Math.ceil((vencimiento.getTime() - hoy.getTime()) / (1000 * 60 * 60 * 24));
    return Math.max(diff, 0);
  });

  estadoClass = computed(() => {
    const contract = this.primaryContract();
    if (!contract) return '';
    switch (contract.estado) {
      case 'ACTIVE': return 'bg-green-100 text-green-800';
      case 'SUSPENDED': return 'bg-red-100 text-red-800';
      case 'CANCELLED': return 'bg-gray-100 text-gray-800';
      case 'EXPIRED': return 'bg-blue-100 text-blue-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  });

  estadoLabel = computed(() => {
    const contract = this.primaryContract();
    if (!contract) return '';
    switch (contract.estado) {
      case 'ACTIVE': return 'ACTIVO';
      case 'SUSPENDED': return 'SUSPENDIDO';
      case 'CANCELLED': return 'CANCELADO';
      case 'EXPIRED': return 'EN RENOVACIÓN';
      default: return contract.estado;
    }
  });

  formatDate(isoDate: string): string {
    const date = new Date(isoDate);
    return date.toLocaleDateString('es-ES', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }
}

import { Component, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CreditResponse } from '../../../../core/models/api-responses.model';
import { ProgressBarComponent } from '../../../../shared/components/progress-bar/progress-bar.component';

@Component({
  selector: 'app-credits-section',
  standalone: true,
  imports: [CommonModule, ProgressBarComponent],
  template: `
    <div class="bg-white rounded-lg shadow p-6">
      <h2 class="text-lg font-semibold mb-4">Créditos</h2>
      @if (creditData(); as data) {
        <dl class="space-y-2 mb-4">
          <div class="flex justify-between">
            <dt class="text-gray-500">Créditos totales</dt>
            <dd class="font-medium">{{ data.creditosTotalesAdquiridos }}</dd>
          </div>
          <div class="flex justify-between">
            <dt class="text-gray-500">Consumidos mes</dt>
            <dd>{{ data.contadorGlobalConsultas }}</dd>
          </div>
          <div class="flex justify-between">
            <dt class="text-gray-500">Consumidos acumulados</dt>
            <dd>{{ consumoAcumulado() }}</dd>
          </div>
          <div class="flex justify-between">
            <dt class="text-gray-500">Saldo disponible</dt>
            <dd class="font-medium">{{ data.saldoDisponible }}</dd>
          </div>
        </dl>
        <div class="mt-3">
          <div class="flex justify-between text-sm text-gray-500 mb-1">
            <span>Consumo</span>
            <span>{{ porcentajeConsumo() | number:'1.0-0' }}%</span>
          </div>
          <app-progress-bar
            [percentage]="porcentajeConsumo()"
            [color]="colorBarra()" />
        </div>
      } @else {
        <p class="text-gray-500">Sin datos de créditos.</p>
      }
    </div>
  `,
})
export class CreditsSectionComponent {
  creditData = input.required<CreditResponse | null>();
  loading = input<boolean>(false);
  error = input<string | null>(null);

  consumoAcumulado = computed(() => {
    const data = this.creditData();
    if (!data) return 0;
    return data.creditosTotalesAdquiridos - data.saldoDisponible;
  });

  porcentajeConsumo = computed(() => {
    const data = this.creditData();
    if (!data || data.creditosTotalesAdquiridos === 0) return 0;
    return (this.consumoAcumulado() / data.creditosTotalesAdquiridos) * 100;
  });

  colorBarra = computed((): 'green' | 'yellow' | 'red' => {
    const pct = this.porcentajeConsumo();
    if (pct <= 80) return 'green';
    if (pct <= 95) return 'yellow';
    return 'red';
  });
}

import { Component, computed, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ContractResponse, CreditResponse } from '../../../../core/models/api-responses.model';

@Component({
  selector: 'app-banner-promo',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  template: `
    @if (showRenovacion()) {
      <div class="p-4 bg-yellow-50 border border-yellow-200 rounded-lg flex items-center justify-between mb-4">
        <div class="flex items-center gap-2">
          <span class="text-yellow-700 font-medium">⚠️ Tu contrato vence pronto. ¡Renueva ahora para no perder el servicio!</span>
        </div>
        <button mat-icon-button (click)="dismiss('renovacion')" aria-label="Cerrar banner de renovación">
          <mat-icon>close</mat-icon>
        </button>
      </div>
    }
    @if (showCreditosBaratos()) {
      <div class="p-4 bg-blue-50 border border-blue-200 rounded-lg flex items-center justify-between mb-4">
        <div class="flex items-center gap-2">
          <span class="text-blue-700 font-medium">💡 Tu saldo de créditos está bajo. ¡Compra un paquete con descuento!</span>
        </div>
        <button mat-icon-button (click)="dismiss('creditos')" aria-label="Cerrar banner de créditos">
          <mat-icon>close</mat-icon>
        </button>
      </div>
    }
  `,
})
export class BannerPromoComponent {
  contractData = input.required<ContractResponse | null>();
  creditData = input.required<CreditResponse | null>();

  dismissedBanners = signal<Set<string>>(new Set());

  showRenovacion = computed(() => {
    const contract = this.contractData();
    if (!contract || this.dismissedBanners().has('renovacion')) return false;
    const primaryContract = contract.contracts[0];
    if (!primaryContract) return false;
    const vencimiento = new Date(primaryContract.fechaVencimientoContrato);
    const hoy = new Date();
    const diasRestantes = Math.ceil(
      (vencimiento.getTime() - hoy.getTime()) / (1000 * 60 * 60 * 24)
    );
    return diasRestantes <= 30 && diasRestantes > 0;
  });

  showCreditosBaratos = computed(() => {
    const credits = this.creditData();
    if (!credits || this.dismissedBanners().has('creditos')) return false;
    const total = credits.creditosTotalesAdquiridos;
    return total > 0 && credits.saldoDisponible <= total * 0.05;
  });

  dismiss(bannerId: string): void {
    this.dismissedBanners.update(set => new Set([...set, bannerId]));
  }
}

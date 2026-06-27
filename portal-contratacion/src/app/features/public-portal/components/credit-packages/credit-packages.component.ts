import { Component, output, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatRadioModule } from '@angular/material/radio';
import { MatCardModule } from '@angular/material/card';
import { PaqueteCreditos, PAQUETES_CREDITOS } from '../../../../core/models/calculator.model';
import { CurrencyEurPipe } from '../../../../shared/pipes/currency-eur.pipe';

@Component({
  selector: 'app-credit-packages',
  standalone: true,
  imports: [CommonModule, MatRadioModule, MatCardModule, CurrencyEurPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div>
      <h2 class="text-lg font-semibold text-gray-800 mb-4">Complementos Opcionales — Paquetes de Créditos</h2>
      <mat-radio-group
        [value]="selectedPackageId()"
        (change)="onPackageChange($event.value)"
        class="flex flex-col gap-3"
        aria-label="Selección de paquete de créditos"
      >
        @for (pkg of packages; track pkg.id) {
          <mat-card class="p-4">
            <mat-radio-button [value]="pkg.id">
              <div class="ml-2">
                <span class="font-medium">{{ pkg.nombre }}</span>
                <span class="text-gray-600 text-sm ml-2">
                  {{ pkg.creditos }} créditos + {{ pkg.bonus }} bonus
                </span>
                <span class="text-primary font-semibold ml-2">
                  {{ pkg.precioAnual | currencyEur }}/año
                </span>
              </div>
            </mat-radio-button>
          </mat-card>
        }
        <mat-card class="p-4">
          <mat-radio-button value="none">
            <span class="ml-2 font-medium">Sin paquete</span>
          </mat-radio-button>
        </mat-card>
      </mat-radio-group>
    </div>
  `,
  styles: []
})
export class CreditPackagesComponent {
  packageChanged = output<PaqueteCreditos | null>();

  readonly packages = PAQUETES_CREDITOS;
  selectedPackageId = signal<string>('none');

  onPackageChange(value: string): void {
    this.selectedPackageId.set(value);
    if (value === 'none') {
      this.packageChanged.emit(null);
    } else {
      const pkg = this.packages.find(p => p.id === value) ?? null;
      this.packageChanged.emit(pkg);
    }
  }
}

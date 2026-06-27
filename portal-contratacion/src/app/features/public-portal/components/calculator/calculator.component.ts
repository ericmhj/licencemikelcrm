import { Component, input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { SubtotalItem } from '../../../../core/models/calculator.model';
import { CurrencyEurPipe } from '../../../../shared/pipes/currency-eur.pipe';

@Component({
  selector: 'app-calculator',
  standalone: true,
  imports: [CommonModule, MatCardModule, CurrencyEurPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title class="text-lg font-semibold">Calculadora</mat-card-title>
      </mat-card-header>
      <mat-card-content class="p-4" aria-live="polite" aria-atomic="true">
        <ul class="space-y-2">
          @for (item of subtotales(); track item.moduloId) {
            <li class="flex justify-between text-sm">
              <span class="text-gray-700">{{ item.nombre }}</span>
              <span class="font-medium">{{ item.subtotalAnual | currencyEur }}/año</span>
            </li>
          }
        </ul>
        <hr class="my-4 border-gray-300" />
        <div class="flex justify-between font-bold text-base">
          <span>TOTAL ANUAL</span>
          <span class="text-primary">{{ totalAnual() | currencyEur }}</span>
        </div>
        <div class="flex justify-between text-sm text-gray-600 mt-2">
          <span>Cuota mensual estimada</span>
          <span>{{ cuotaMensual() | currencyEur }}</span>
        </div>
      </mat-card-content>
    </mat-card>
  `,
  styles: []
})
export class CalculatorComponent {
  subtotales = input.required<SubtotalItem[]>();
  totalAnual = input.required<number>();
  cuotaMensual = input.required<number>();
}

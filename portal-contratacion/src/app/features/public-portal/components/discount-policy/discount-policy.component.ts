import { Component, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-discount-policy',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div>
      <h2 class="text-lg font-semibold text-gray-800 mb-4">Política de Descuentos</h2>
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <!-- 10% discount -->
        <mat-card class="text-center p-4">
          <mat-card-content>
            <mat-icon class="text-success text-4xl mb-2" style="font-size: 40px; width: 40px; height: 40px;">
              savings
            </mat-icon>
            <p class="text-2xl font-bold text-success">-10%</p>
            <p class="text-sm font-medium text-gray-800 mt-1">Antes del límite</p>
            <p class="text-xs text-gray-600 mt-1">Pago anticipado antes de la fecha límite</p>
          </mat-card-content>
        </mat-card>

        <!-- 3% discount -->
        <mat-card class="text-center p-4">
          <mat-card-content>
            <mat-icon class="text-warning text-4xl mb-2" style="font-size: 40px; width: 40px; height: 40px;">
              today
            </mat-icon>
            <p class="text-2xl font-bold text-warning">-3%</p>
            <p class="text-sm font-medium text-gray-800 mt-1">En fecha límite</p>
            <p class="text-xs text-gray-600 mt-1">Pago exacto el día de vencimiento</p>
          </mat-card-content>
        </mat-card>

        <!-- 0% discount -->
        <mat-card class="text-center p-4">
          <mat-card-content>
            <mat-icon class="text-danger text-4xl mb-2" style="font-size: 40px; width: 40px; height: 40px;">
              event_busy
            </mat-icon>
            <p class="text-2xl font-bold text-danger">0%</p>
            <p class="text-sm font-medium text-gray-800 mt-1">Después</p>
            <p class="text-xs text-gray-600 mt-1">Pago posterior a la fecha límite</p>
          </mat-card-content>
        </mat-card>
      </div>
    </div>
  `,
  styles: []
})
export class DiscountPolicyComponent {}

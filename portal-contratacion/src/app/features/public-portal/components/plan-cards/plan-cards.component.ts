import { Component, input, output, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatRadioModule } from '@angular/material/radio';
import { Plan } from '../../../../core/services/plan.service';

/**
 * Tarjetas de PLANES disponibles (cargados de /api/v1/plans, los mismos que
 * administra /admin/planes). Selección ÚNICA mediante RadioButton: el contrato
 * se genera con el plan elegido y la calculadora recalcula con su precio.
 */
@Component({
  selector: 'app-plan-cards',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatRadioModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div>
      <h2 class="text-lg font-semibold text-gray-800 mb-4">Planes Disponibles</h2>

      @if (plans().length === 0) {
        <p class="text-sm text-gray-500">Cargando planes...</p>
      }

      <mat-radio-group
        class="grid grid-cols-1 sm:grid-cols-2 gap-4"
        [value]="selectedCodigo()"
        (change)="onSelect($event.value)"
      >
        @for (plan of plans(); track plan.codigo) {
          <mat-card
            class="cursor-pointer transition-all"
            [class.border-2]="selectedCodigo() === plan.codigo"
            [class.border-primary]="selectedCodigo() === plan.codigo"
            (click)="onSelect(plan.codigo)"
            [attr.tabindex]="0"
            (keydown.enter)="onSelect(plan.codigo)"
            (keydown.space)="onSelect(plan.codigo); $event.preventDefault()"
          >
            <mat-card-content class="p-4">
              <div class="flex items-start gap-3">
                <mat-radio-button
                  [value]="plan.codigo"
                  (click)="$event.stopPropagation()"
                  [attr.aria-label]="'Seleccionar plan ' + plan.nombre"
                />
                <div class="flex-1">
                  <h3 class="font-medium text-gray-900">{{ plan.nombre }}</h3>
                  <p class="text-sm text-gray-600 mt-1">{{ plan.descripcion }}</p>
                  <p class="text-primary font-semibold mt-2">
                    {{ plan.precioMensual | currency:'MXN':'symbol':'1.2-2' }}/mes
                  </p>
                  <ul class="text-xs text-gray-500 mt-2 space-y-0.5">
                    <li>
                      Créditos/mes:
                      {{ plan.creditosMensuales === -1 ? 'Ilimitado' : plan.creditosMensuales }}
                    </li>
                    <li>
                      Usuarios:
                      {{ plan.maxUsuarios === -1 ? 'Ilimitado' : plan.maxUsuarios }}
                    </li>
                  </ul>
                </div>
              </div>
            </mat-card-content>
          </mat-card>
        }
      </mat-radio-group>
    </div>
  `,
  styles: [`
    mat-card:focus { outline: 2px solid var(--color-primary, #1976D2); outline-offset: 2px; }
  `]
})
export class PlanCardsComponent {
  plans = input.required<Plan[]>();
  selectedCodigo = input<string | null>(null);
  planSelected = output<Plan>();

  onSelect(codigo: string): void {
    const plan = this.plans().find(p => p.codigo === codigo);
    if (plan) this.planSelected.emit(plan);
  }
}

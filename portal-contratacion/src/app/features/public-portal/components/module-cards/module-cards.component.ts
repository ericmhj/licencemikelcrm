import { Component, signal, input, output, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { ModuloDisponible } from '../../../../core/models/calculator.model';
import { CurrencyEurPipe } from '../../../../shared/pipes/currency-eur.pipe';

@Component({
  selector: 'app-module-cards',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatCheckboxModule, CurrencyEurPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div>
      <h2 class="text-lg font-semibold text-gray-800 mb-4">Módulos Disponibles</h2>
      <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
        @for (mod of modules(); track mod.id) {
          <mat-card
            class="cursor-pointer transition-all"
            [class.border-2]="isSelected(mod.id)"
            [class.border-primary]="isSelected(mod.id)"
            [class.opacity-75]="mod.obligatorio"
            [attr.role]="'checkbox'"
            [attr.aria-checked]="isSelected(mod.id)"
            [attr.aria-label]="mod.nombre + ' - ' + mod.precioMensual + ' €/mes'"
            (click)="toggleModule(mod)"
            (keydown.enter)="toggleModule(mod)"
            (keydown.space)="toggleModule(mod); $event.preventDefault()"
            [attr.tabindex]="0"
          >
            <mat-card-content class="p-4">
              <div class="flex items-start gap-3">
                <mat-checkbox
                  [checked]="isSelected(mod.id)"
                  [disabled]="mod.obligatorio"
                  (click)="$event.stopPropagation()"
                  (change)="toggleModule(mod)"
                  [attr.aria-label]="'Seleccionar ' + mod.nombre"
                />
                <div class="flex-1">
                  <h3 class="font-medium text-gray-900">{{ mod.nombre }}</h3>
                  <p class="text-sm text-gray-600 mt-1">{{ mod.descripcion }}</p>
                  <p class="text-primary font-semibold mt-2">{{ mod.precioMensual | currencyEur }}/mes</p>
                </div>
              </div>
            </mat-card-content>
          </mat-card>
        }
      </div>
    </div>
  `,
  styles: [`
    mat-card:focus {
      outline: 2px solid var(--color-primary, #1976D2);
      outline-offset: 2px;
    }
  `]
})
export class ModuleCardsComponent {
  modules = input.required<ModuloDisponible[]>();
  selectionChanged = output<ModuloDisponible[]>();

  selectedModules = signal<Set<string>>(new Set(['CRM_BASE']));

  toggleModule(mod: ModuloDisponible): void {
    if (mod.obligatorio) return;
    this.selectedModules.update(set => {
      const next = new Set(set);
      if (next.has(mod.id)) {
        next.delete(mod.id);
      } else {
        next.add(mod.id);
      }
      return next;
    });
    this.emitSelection();
  }

  isSelected(moduleId: string): boolean {
    return this.selectedModules().has(moduleId);
  }

  private emitSelection(): void {
    const selected = this.modules().filter(m => this.selectedModules().has(m.id));
    this.selectionChanged.emit(selected);
  }
}

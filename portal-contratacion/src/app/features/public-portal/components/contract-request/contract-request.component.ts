import { Component, input, inject, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { ModuloDisponible, PaqueteCreditos } from '../../../../core/models/calculator.model';

@Component({
  selector: 'app-contract-request',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatCardModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <mat-card>
      <mat-card-content class="p-4">
        <button
          mat-raised-button
          color="primary"
          class="w-full min-h-[44px]"
          (click)="solicitarContrato()"
          [disabled]="!isValid()"
          aria-label="Solicitar contrato con los módulos seleccionados"
        >
          Solicitar contrato
          <mat-icon>arrow_forward</mat-icon>
        </button>
      </mat-card-content>
    </mat-card>
  `,
  styles: []
})
export class ContractRequestComponent {
  private readonly router = inject(Router);

  selectedModules = input.required<ModuloDisponible[]>();
  selectedPackage = input.required<PaqueteCreditos | null>();

  isValid(): boolean {
    // At least CRM_BASE must be selected (always true but defensive)
    return this.selectedModules().some(m => m.id === 'CRM_BASE');
  }

  solicitarContrato(): void {
    if (!this.isValid()) return;

    const moduleIds = this.selectedModules().map(m => m.id).join(',');
    const pkg = this.selectedPackage();

    const queryParams: Record<string, string> = {
      modules: moduleIds,
    };
    if (pkg) {
      queryParams['package'] = pkg.id;
    }

    this.router.navigate(['/contratacion', 'solicitud'], { queryParams });
  }
}

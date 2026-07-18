import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { PlanService, Plan } from '../../../core/services/plan.service';

@Component({
  selector: 'app-plan-list',
  standalone: true,
  imports: [CommonModule, RouterLink, MatTableModule, MatButtonModule, MatIconModule, MatChipsModule, MatCardModule, MatSnackBarModule],
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title>Administración de Planes</mat-card-title>
        <mat-card-subtitle>Catálogo de planes y roles autorizados</mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        <div class="actions-bar">
          <a mat-raised-button color="primary" routerLink="nuevo">
            <mat-icon>add</mat-icon> Nuevo Plan
          </a>
        </div>

        <table mat-table [dataSource]="plans" class="full-width">
          <ng-container matColumnDef="codigo">
            <th mat-header-cell *matHeaderCellDef>Código</th>
            <td mat-cell *matCellDef="let plan">{{ plan.codigo }}</td>
          </ng-container>

          <ng-container matColumnDef="nombre">
            <th mat-header-cell *matHeaderCellDef>Nombre</th>
            <td mat-cell *matCellDef="let plan">{{ plan.nombre }}</td>
          </ng-container>

          <ng-container matColumnDef="creditos">
            <th mat-header-cell *matHeaderCellDef>Créditos/mes</th>
            <td mat-cell *matCellDef="let plan">
              {{ plan.creditosMensuales === -1 ? 'Ilimitado' : plan.creditosMensuales }}
            </td>
          </ng-container>

          <ng-container matColumnDef="usuarios">
            <th mat-header-cell *matHeaderCellDef>Máx. Usuarios</th>
            <td mat-cell *matCellDef="let plan">
              {{ plan.maxUsuarios === -1 ? 'Ilimitado' : plan.maxUsuarios }}
            </td>
          </ng-container>

          <ng-container matColumnDef="roles">
            <th mat-header-cell *matHeaderCellDef>Roles Autorizados</th>
            <td mat-cell *matCellDef="let plan">
              <mat-chip-set>
                @for (role of plan.rolesAutorizados; track role) {
                  <mat-chip>{{ role }}</mat-chip>
                }
              </mat-chip-set>
            </td>
          </ng-container>

          <ng-container matColumnDef="precio">
            <th mat-header-cell *matHeaderCellDef>Precio/mes</th>
            <td mat-cell *matCellDef="let plan">\${{ plan.precioMensual | number:'1.2-2' }}</td>
          </ng-container>

          <ng-container matColumnDef="acciones">
            <th mat-header-cell *matHeaderCellDef>Acciones</th>
            <td mat-cell *matCellDef="let plan">
              <a mat-icon-button [routerLink]="['editar', plan.codigo]" matTooltip="Editar">
                <mat-icon>edit</mat-icon>
              </a>
              <button mat-icon-button color="warn" (click)="deactivate(plan)" matTooltip="Desactivar">
                <mat-icon>delete</mat-icon>
              </button>
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
        </table>
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .actions-bar { margin-bottom: 16px; }
    .full-width { width: 100%; }
    mat-chip { font-size: 12px; }
  `]
})
export class PlanListComponent implements OnInit {
  private planService = inject(PlanService);
  private snackBar = inject(MatSnackBar);

  plans: Plan[] = [];
  displayedColumns = ['codigo', 'nombre', 'creditos', 'usuarios', 'roles', 'precio', 'acciones'];

  ngOnInit() {
    this.loadPlans();
  }

  loadPlans() {
    this.planService.getAll().subscribe({
      next: (plans) => this.plans = plans,
      error: () => this.snackBar.open('Error cargando planes', 'Cerrar', { duration: 3000 })
    });
  }

  deactivate(plan: Plan) {
    if (confirm(`¿Desactivar el plan "${plan.nombre}"?`)) {
      this.planService.deactivate(plan.codigo).subscribe({
        next: () => {
          this.snackBar.open('Plan desactivado', 'OK', { duration: 3000 });
          this.loadPlans();
        },
        error: () => this.snackBar.open('Error al desactivar', 'Cerrar', { duration: 3000 })
      });
    }
  }
}

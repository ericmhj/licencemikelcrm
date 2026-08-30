import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { EstadoCuentaService } from '../../../core/services/estado-cuenta.service';
import { TenantSummary } from '../../../core/models/cartera.model';

@Component({
  selector: 'app-estado-cuenta-list',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatChipsModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title>
          <mat-icon class="title-icon">receipt_long</mat-icon>
          Estados de Cuenta
        </mat-card-title>
        <mat-card-subtitle>
          Consulte el estado de cuenta (movimientos fiscalizables) de cada tenant.
        </mat-card-subtitle>
      </mat-card-header>

      <mat-card-content>
        @if (loading) {
          <div class="loading-state">
            <mat-spinner diameter="40"></mat-spinner>
            <p>Cargando tenants...</p>
          </div>
        }

        @if (!loading && tenants.length > 0) {
          <table mat-table [dataSource]="tenants" class="full-width">
            <ng-container matColumnDef="nombre">
              <th mat-header-cell *matHeaderCellDef>Nombre</th>
              <td mat-cell *matCellDef="let t">{{ t.nombre }}</td>
            </ng-container>

            <ng-container matColumnDef="slug">
              <th mat-header-cell *matHeaderCellDef>Contacto</th>
              <td mat-cell *matCellDef="let t">
                <code class="slug-badge">{{ t.slug }}</code>
              </td>
            </ng-container>

            <ng-container matColumnDef="plan">
              <th mat-header-cell *matHeaderCellDef>Plan</th>
              <td mat-cell *matCellDef="let t">
                <mat-chip>{{ t.plan }}</mat-chip>
              </td>
            </ng-container>

            <ng-container matColumnDef="estado">
              <th mat-header-cell *matHeaderCellDef>Estado</th>
              <td mat-cell *matCellDef="let t">
                <span [class]="'estado-badge estado-' + t.estado">{{ t.estado }}</span>
              </td>
            </ng-container>

            <ng-container matColumnDef="acciones">
              <th mat-header-cell *matHeaderCellDef>Acciones</th>
              <td mat-cell *matCellDef="let t">
                <button mat-icon-button color="primary" (click)="openDetail(t)" matTooltip="Ver estado de cuenta">
                  <mat-icon>visibility</mat-icon>
                </button>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
          </table>
        }

        @if (!loading && tenants.length === 0) {
          <div class="empty-state">
            <mat-icon>inventory_2</mat-icon>
            <p>No hay tenants registrados</p>
          </div>
        }
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .title-icon { vertical-align: middle; margin-right: 8px; }
    .full-width { width: 100%; }
    .slug-badge { background: #f1f5f9; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
    .estado-badge { padding: 2px 10px; border-radius: 12px; font-size: 11px; font-weight: 600; text-transform: uppercase; }
    .estado-active, .estado-ACTIVE { background: #dcfce7; color: #166534; }
    .estado-suspended, .estado-SUSPENDED { background: #fef2f2; color: #991b1b; }
    .empty-state { text-align: center; padding: 48px; color: #6b7280; }
    .empty-state mat-icon { font-size: 48px; width: 48px; height: 48px; margin-bottom: 8px; }
    .loading-state { text-align: center; padding: 48px; }
  `],
})
export class EstadoCuentaListComponent implements OnInit {
  private estadoCuentaService = inject(EstadoCuentaService);
  private snackBar = inject(MatSnackBar);
  private router = inject(Router);

  tenants: TenantSummary[] = [];
  loading = true;
  displayedColumns = ['nombre', 'slug', 'plan', 'estado', 'acciones'];

  ngOnInit(): void {
    this.estadoCuentaService.listTenants().subscribe({
      next: (results) => {
        this.tenants = results;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.snackBar.open(err?.error?.message || 'Error cargando tenants', 'Cerrar', { duration: 4000 });
      },
    });
  }

  openDetail(tenant: TenantSummary): void {
    this.router.navigate(['/admin/estados-cuenta', tenant.id]);
  }
}

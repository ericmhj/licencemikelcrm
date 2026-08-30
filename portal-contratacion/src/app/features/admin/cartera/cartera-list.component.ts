import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CarteraService } from '../../../core/services/cartera.service';
import { TenantSummary } from '../../../core/models/cartera.model';

@Component({
  selector: 'app-cartera-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
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
          <mat-icon class="title-icon">account_balance_wallet</mat-icon>
          Administración de Cartera por Tenant
        </mat-card-title>
        <mat-card-subtitle>
          Consulte y administre el saldo de créditos de cada tenant. Las modificaciones quedan registradas en el audit log.
        </mat-card-subtitle>
      </mat-card-header>

      <mat-card-content>
        <!-- Search bar -->
        <div class="search-section">
          <mat-form-field appearance="outline" class="search-field">
            <mat-label>Filtrar por nombre o slug</mat-label>
            <input matInput
                   [(ngModel)]="searchQuery"
                   (keyup.enter)="search()"
                   (input)="filterLocal()"
                   placeholder="Ej: acme, empresa..."
                   autocomplete="off">
            <mat-icon matSuffix>search</mat-icon>
          </mat-form-field>
        </div>

        <!-- Loading -->
        @if (loading) {
          <div class="loading-state">
            <mat-spinner diameter="40"></mat-spinner>
            <p>Cargando tenants...</p>
          </div>
        }

        <!-- Results -->
        @if (!loading && filteredTenants.length > 0) {
          <table mat-table [dataSource]="filteredTenants" class="full-width">
            <ng-container matColumnDef="nombre">
              <th mat-header-cell *matHeaderCellDef>Nombre</th>
              <td mat-cell *matCellDef="let t">{{ t.nombre }}</td>
            </ng-container>

            <ng-container matColumnDef="slug">
              <th mat-header-cell *matHeaderCellDef>Slug</th>
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
                <button mat-icon-button color="primary" (click)="openDetail(t)" matTooltip="Ver cartera">
                  <mat-icon>visibility</mat-icon>
                </button>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
          </table>
        }

        @if (!loading && filteredTenants.length === 0 && allTenants.length > 0) {
          <div class="empty-state">
            <mat-icon>search_off</mat-icon>
            <p>No se encontraron tenants para "{{ searchQuery }}"</p>
          </div>
        }

        @if (!loading && allTenants.length === 0) {
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
    .search-section { display: flex; gap: 12px; align-items: center; margin-bottom: 16px; }
    .search-field { flex: 1; max-width: 500px; }
    .full-width { width: 100%; }
    .slug-badge { background: #f1f5f9; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
    .estado-badge { padding: 2px 10px; border-radius: 12px; font-size: 11px; font-weight: 600; text-transform: uppercase; }
    .estado-active, .estado-ACTIVE { background: #dcfce7; color: #166534; }
    .estado-suspended, .estado-SUSPENDED { background: #fef2f2; color: #991b1b; }
    .estado-pending_deletion { background: #fef9c3; color: #854d0e; }
    .empty-state { text-align: center; padding: 48px; color: #6b7280; }
    .empty-state mat-icon { font-size: 48px; width: 48px; height: 48px; margin-bottom: 8px; }
    .loading-state { text-align: center; padding: 48px; }
  `],
})
export class CarteraListComponent implements OnInit {
  private carteraService = inject(CarteraService);
  private snackBar = inject(MatSnackBar);
  private router = inject(Router);

  searchQuery = '';
  allTenants: TenantSummary[] = [];
  filteredTenants: TenantSummary[] = [];
  loading = true;

  displayedColumns = ['nombre', 'slug', 'plan', 'estado', 'acciones'];

  ngOnInit(): void {
    this.loadAllTenants();
  }

  loadAllTenants(): void {
    this.loading = true;
    this.carteraService.listAllTenants().subscribe({
      next: (results) => {
        this.allTenants = results;
        this.filteredTenants = results;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.snackBar.open(
          err?.error?.message || 'Error cargando tenants',
          'Cerrar',
          { duration: 4000 },
        );
      },
    });
  }

  filterLocal(): void {
    const q = this.searchQuery.trim().toLowerCase();
    if (!q) {
      this.filteredTenants = this.allTenants;
      return;
    }
    this.filteredTenants = this.allTenants.filter(
      t => t.nombre.toLowerCase().includes(q) || t.slug.toLowerCase().includes(q),
    );
  }

  search(): void {
    this.filterLocal();
  }

  openDetail(tenant: TenantSummary): void {
    this.router.navigate(['/admin/cartera', tenant.id]);
  }
}

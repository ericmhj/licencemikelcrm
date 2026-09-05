import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CarteraService } from '../../../core/services/cartera.service';
import { PlanService, Plan } from '../../../core/services/plan.service';
import { TenantSummary } from '../../../core/models/cartera.model';

interface TenantRow extends TenantSummary {
  /** Plan seleccionado en el dropdown (código del plan). */
  selectedPlanCode: string;
  /** true mientras se guarda el cambio de este tenant. */
  saving: boolean;
}

@Component({
  selector: 'app-planes-tenants',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatFormFieldModule,
    MatInputModule,
    MatSnackBarModule,
  ],
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title>Planes por Tenant</mat-card-title>
        <mat-card-subtitle>Asigna el plan de facturación a cada tenant</mat-card-subtitle>
      </mat-card-header>

      <mat-card-content>
        @if (loading()) {
          <div class="center"><mat-spinner diameter="40"></mat-spinner></div>
        } @else if (error()) {
          <div class="error-box">
            <mat-icon>error_outline</mat-icon>
            <span>{{ error() }}</span>
            <button mat-button color="primary" (click)="load()">Reintentar</button>
          </div>
        } @else {
          <mat-form-field appearance="outline" class="search-field">
            <mat-label>Buscar tenant</mat-label>
            <input matInput
                   [(ngModel)]="searchTerm"
                   (ngModelChange)="onSearchChange($event)"
                   placeholder="Nombre, slug o plan" />
            <mat-icon matPrefix>search</mat-icon>
            @if (searchTerm) {
              <button mat-icon-button matSuffix (click)="clearSearch()" aria-label="Limpiar búsqueda">
                <mat-icon>close</mat-icon>
              </button>
            }
          </mat-form-field>

          <table mat-table [dataSource]="filteredTenants()" class="full-width">
            <ng-container matColumnDef="nombre">
              <th mat-header-cell *matHeaderCellDef>Tenant</th>
              <td mat-cell *matCellDef="let t">
                <div class="tenant-name">{{ t.nombre }}</div>
                <div class="tenant-slug">{{ t.slug }}</div>
              </td>
            </ng-container>

            <ng-container matColumnDef="estado">
              <th mat-header-cell *matHeaderCellDef>Estado</th>
              <td mat-cell *matCellDef="let t">
                <!-- Etiqueta solo informativa: Activo o Suspendido -->
                <mat-chip [class]="'estado-' + estadoNormalizado(t.estado)">
                  {{ estadoLabel(t.estado) }}
                </mat-chip>
              </td>
            </ng-container>

            <ng-container matColumnDef="planActual">
              <th mat-header-cell *matHeaderCellDef>Plan actual</th>
              <td mat-cell *matCellDef="let t">
                @if (t.plan && t.plan !== 'sin-plan') {
                  <span>{{ planName(t.plan) }}</span>
                } @else {
                  <span class="sin-plan">Sin plan</span>
                }
              </td>
            </ng-container>

            <ng-container matColumnDef="asignar">
              <th mat-header-cell *matHeaderCellDef>Asignar plan</th>
              <td mat-cell *matCellDef="let t">
                <mat-select [(ngModel)]="t.selectedPlanCode" class="plan-select" placeholder="Selecciona un plan">
                  @for (p of planes(); track p.codigo) {
                    <mat-option [value]="p.codigo">{{ p.nombre }}</mat-option>
                  }
                </mat-select>
              </td>
            </ng-container>

            <ng-container matColumnDef="acciones">
              <th mat-header-cell *matHeaderCellDef></th>
              <td mat-cell *matCellDef="let t">
                <button mat-raised-button color="primary"
                        [disabled]="t.saving || !t.selectedPlanCode || t.selectedPlanCode === t.plan"
                        (click)="assign(t)">
                  @if (t.saving) {
                    <mat-spinner diameter="18"></mat-spinner>
                  } @else {
                    <span>Guardar</span>
                  }
                </button>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
          </table>

          @if (filteredTenants().length === 0) {
            <p class="empty">
              {{ searchTerm ? 'No hay tenants que coincidan con la búsqueda.' : 'No hay tenants registrados.' }}
            </p>
          }
        }
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .full-width { width: 100%; }
    .search-field { width: 100%; max-width: 420px; margin-bottom: 8px; }
    .center { display: flex; justify-content: center; padding: 40px; }
    .error-box { display: flex; align-items: center; gap: 8px; color: #b00020; padding: 16px; }
    .tenant-name { font-weight: 600; }
    .tenant-slug { font-size: 12px; color: #6b7280; }
    .sin-plan { color: #b00020; font-style: italic; }
    .plan-select { min-width: 200px; }
    .empty { text-align: center; color: #6b7280; padding: 24px; }
    mat-chip.estado-active { background: #dcfce7; color: #166534; }
    mat-chip.estado-suspended { background: #fee2e2; color: #991b1b; }
    td.mat-mdc-cell { padding: 12px 8px; }
  `],
})
export class PlanesTenantsComponent implements OnInit {
  private carteraService = inject(CarteraService);
  private planService = inject(PlanService);
  private snackBar = inject(MatSnackBar);

  tenants = signal<TenantRow[]>([]);
  filteredTenants = signal<TenantRow[]>([]);
  planes = signal<Plan[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  searchTerm = '';

  displayedColumns = ['nombre', 'estado', 'planActual', 'asignar', 'acciones'];

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);

    // Cargar catálogo de planes y lista de tenants en paralelo.
    this.planService.getAll().subscribe({
      next: (plans) => {
        this.planes.set(plans.filter((p) => p.activo));
        this.loadTenants();
      },
      error: () => {
        this.error.set('Error cargando el catálogo de planes.');
        this.loading.set(false);
      },
    });
  }

  private loadTenants(): void {
    this.carteraService.listAllTenants().subscribe({
      next: (list) => {
        this.tenants.set(
          list.map((t) => ({
            ...t,
            selectedPlanCode: t.plan && t.plan !== 'sin-plan' ? t.plan : '',
            saving: false,
          })),
        );
        this.applyFilter();
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error cargando los tenants.');
        this.loading.set(false);
      },
    });
  }

  planName(codigo: string): string {
    return this.planes().find((p) => p.codigo === codigo)?.nombre ?? codigo;
  }

  /**
   * Homologa el estado del tenant a solo dos valores: 'active' o 'suspended'.
   * Cualquier estado distinto de ACTIVE (SUSPENDED, ONBOARDING, CANCELLED) se
   * considera suspendido. La etiqueta es solo informativa; no dispara acciones.
   */
  estadoNormalizado(estado: string): 'active' | 'suspended' {
    return (estado ?? '').toUpperCase() === 'ACTIVE' ? 'active' : 'suspended';
  }

  estadoLabel(estado: string): string {
    return this.estadoNormalizado(estado) === 'active' ? 'Activo' : 'Suspendido';
  }

  onSearchChange(term: string): void {
    this.searchTerm = term;
    this.applyFilter();
  }

  clearSearch(): void {
    this.searchTerm = '';
    this.applyFilter();
  }

  /** Filtra por nombre, slug o nombre del plan (case-insensitive). */
  private applyFilter(): void {
    const term = this.searchTerm.trim().toLowerCase();
    if (!term) {
      this.filteredTenants.set(this.tenants());
      return;
    }
    this.filteredTenants.set(
      this.tenants().filter((t) => {
        const planNombre = this.planName(t.plan).toLowerCase();
        return (
          t.nombre.toLowerCase().includes(term) ||
          (t.slug ?? '').toLowerCase().includes(term) ||
          planNombre.includes(term)
        );
      }),
    );
  }

  assign(t: TenantRow): void {
    const plan = this.planes().find((p) => p.codigo === t.selectedPlanCode);
    if (!plan) {
      this.snackBar.open('Selecciona un plan válido', 'Cerrar', { duration: 3000 });
      return;
    }

    t.saving = true;
    this.carteraService.updateTenantPlan(t.id, plan.id).subscribe({
      next: () => {
        t.saving = false;
        t.plan = plan.codigo;
        this.snackBar.open(`Plan "${plan.nombre}" asignado a ${t.nombre}`, 'OK', { duration: 3000 });
      },
      error: () => {
        t.saving = false;
        this.snackBar.open('Error al asignar el plan', 'Cerrar', { duration: 4000 });
      },
    });
  }
}

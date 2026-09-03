import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { EstadoCuentaService } from '../../../core/services/estado-cuenta.service';
import { MovimientoEdoCuenta, TenantEdoCuentaResumen } from '../../../core/models/estado-cuenta.model';

@Component({
  selector: 'app-estado-cuenta-detail',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatChipsModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <div class="detail-container">
      <a mat-button routerLink="/admin/estados-cuenta" class="back-btn">
        <mat-icon>arrow_back</mat-icon> Volver
      </a>

      @if (loading) {
        <div class="loading-state">
          <mat-spinner diameter="40"></mat-spinner>
          <p>Cargando estado de cuenta...</p>
        </div>
      }

      @if (resumen && !loading) {
        <!-- Resumen -->
        <mat-card class="resumen-card">
          <mat-card-header>
            <mat-card-title>
              <mat-icon>business</mat-icon>
              {{ resumen.nombre }}
            </mat-card-title>
            <mat-card-subtitle>Estado de Cuenta — Movimientos fiscalizables</mat-card-subtitle>
          </mat-card-header>

          <div class="tenant-meta">
            <span class="chip chip-plan">Plan: {{ resumen.plan }}</span>
            <span class="chip" [class.chip-active]="resumen.estado === 'ACTIVE'" [class.chip-suspended]="resumen.estado === 'SUSPENDED'">
              {{ resumen.estado === 'ACTIVE' ? 'Activo' : resumen.estado === 'SUSPENDED' ? 'Suspendido' : resumen.estado }}
            </span>
            @if (resumen.servicioPagadoHasta) {
              <span class="chip chip-info">Servicio pagado hasta: {{ resumen.servicioPagadoHasta }}</span>
            }
          </div>

          @if (resumen.estado === 'SUSPENDED') {
            <div class="suspended-notice">
              <mat-icon>block</mat-icon>
              Tenant suspendido por impago. Regularice el pago mensual (SPEI) para reactivar el servicio y los ajustes de cartera.
            </div>
          }

          <mat-card-content>
            <div class="metrics-grid">
              <div class="metric metric-saldo">
                <span class="metric-value" [class.negative]="resumen.saldoActual < 0">
                  {{ resumen.saldoActual | currency:'MXN':'symbol':'1.2-2' }}
                </span>
                <span class="metric-label">Saldo actual</span>
              </div>
              <div class="metric">
                <span class="metric-value positive">
                  {{ resumen.totalAbonos | currency:'MXN':'symbol':'1.2-2' }}
                </span>
                <span class="metric-label">Total abonos</span>
              </div>
              <div class="metric">
                <span class="metric-value negative">
                  {{ resumen.totalCargos | currency:'MXN':'symbol':'1.2-2' }}
                </span>
                <span class="metric-label">Total cargos</span>
              </div>
            </div>

            <!-- Adeudo de mensualidad del mes en curso -->
            <div class="adeudo-banner" [class.al-corriente]="resumen.mesPagado" [class.moroso]="!resumen.mesPagado">
              <mat-icon>{{ resumen.mesPagado ? 'check_circle' : 'warning' }}</mat-icon>
              @if (resumen.mesPagado) {
                <span>Mensualidad del período {{ resumen.periodoMes }} pagada.</span>
              } @else {
                <span>
                  <strong>Adeudo de mensualidad ({{ resumen.periodoMes }}):</strong>
                  {{ resumen.adeudoMes | currency:'MXN':'symbol':'1.2-2' }}
                  &nbsp;— pago pendiente.
                </span>
              }
            </div>
          </mat-card-content>
        </mat-card>

        <!-- Movimientos -->
        <mat-card class="movimientos-card">
          <mat-card-header>
            <mat-card-title>Movimientos</mat-card-title>
          </mat-card-header>

          <mat-card-content>
            <div class="filters-bar">
              <mat-form-field appearance="outline" class="filter-field">
                <mat-label>Tipo</mat-label>
                <mat-select [(ngModel)]="filterTipo" (selectionChange)="applyFilters()">
                  <mat-option value="">Todos</mat-option>
                  <mat-option value="PAGO_RENTA">Pago Renta CRM</mat-option>
                  <mat-option value="ABONO">Abonos</mat-option>
                  <mat-option value="CARGO">Cargos</mat-option>
                </mat-select>
              </mat-form-field>
              <mat-form-field appearance="outline" class="filter-field">
                <mat-label>Desde</mat-label>
                <input matInput type="date" [(ngModel)]="filterDesde" (change)="applyFilters()" />
              </mat-form-field>
              <mat-form-field appearance="outline" class="filter-field">
                <mat-label>Hasta</mat-label>
                <input matInput type="date" [(ngModel)]="filterHasta" (change)="applyFilters()" />
              </mat-form-field>
              @if (filterTipo || filterDesde || filterHasta) {
                <button mat-button (click)="clearFilters()">Limpiar</button>
              }
            </div>

            <table mat-table [dataSource]="movimientos" class="full-width">
              <ng-container matColumnDef="tipo">
                <th mat-header-cell *matHeaderCellDef>Tipo</th>
                <td mat-cell *matCellDef="let m">
                  <span [class]="'tipo-badge tipo-' + m.tipo">
                    {{ tipoIcono(m.tipo) }} {{ tipoLabel(m.tipo) }}
                  </span>
                </td>
              </ng-container>

              <ng-container matColumnDef="concepto">
                <th mat-header-cell *matHeaderCellDef>Concepto</th>
                <td mat-cell *matCellDef="let m">{{ m.concepto }}</td>
              </ng-container>

              <ng-container matColumnDef="monto">
                <th mat-header-cell *matHeaderCellDef>Monto</th>
                <td mat-cell *matCellDef="let m">
                  <span [class]="m.monto >= 0 ? 'amount-positive' : 'amount-negative'">
                    {{ m.monto | currency:'MXN':'symbol':'1.2-2' }}
                  </span>
                </td>
              </ng-container>

              <ng-container matColumnDef="saldoResultante">
                <th mat-header-cell *matHeaderCellDef>Saldo</th>
                <td mat-cell *matCellDef="let m">
                  {{ m.saldoResultante | currency:'MXN':'symbol':'1.2-2' }}
                </td>
              </ng-container>

              <ng-container matColumnDef="referencia">
                <th mat-header-cell *matHeaderCellDef>Referencia</th>
                <td mat-cell *matCellDef="let m">
                  <code class="ref-badge">{{ m.claveRastreo || m.referencia || '—' }}</code>
                </td>
              </ng-container>

              <ng-container matColumnDef="fecha">
                <th mat-header-cell *matHeaderCellDef>Fecha</th>
                <td mat-cell *matCellDef="let m">{{ m.registradoEn | date:'short':'-0600':'es-MX' }}</td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="columns"></tr>
              <tr mat-row *matRowDef="let row; columns: columns;"></tr>
            </table>

            @if (movimientos.length === 0 && !movLoading) {
              <div class="empty-state">
                <p>Sin movimientos registrados</p>
              </div>
            }

            <mat-paginator
              [length]="totalMovimientos"
              [pageSize]="pageSize"
              [pageIndex]="currentPage - 1"
              [pageSizeOptions]="[10, 20, 50]"
              (page)="onPageChange($event)"
              showFirstLastButtons>
            </mat-paginator>
          </mat-card-content>
        </mat-card>
      }
    </div>
  `,
  styles: [`
    .detail-container { max-width: 1000px; }
    .back-btn { margin-bottom: 16px; }
    .loading-state { text-align: center; padding: 48px; }
    .resumen-card { margin-bottom: 24px; }
    .tenant-meta { display: flex; flex-wrap: wrap; gap: 8px; padding: 0 16px 8px; }
    .chip { padding: 4px 10px; border-radius: 12px; font-size: 12px; font-weight: 600; background: #f1f5f9; color: #334155; }
    .chip-plan { background: #eef2ff; color: #3730a3; }
    .chip-active { background: #dcfce7; color: #166534; }
    .chip-suspended { background: #fef2f2; color: #991b1b; }
    .chip-info { background: #f0f9ff; color: #075985; }
    .suspended-notice { display: flex; align-items: center; gap: 8px; margin: 0 16px 12px; padding: 10px 14px; background: #fef2f2; border: 1px solid #fecaca; color: #991b1b; border-radius: 8px; font-size: 13px; }
    .metrics-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin: 16px 0; }
    .metric { background: #f8fafc; border-radius: 8px; padding: 16px; text-align: center; }
    .adeudo-banner { display: flex; align-items: center; gap: 8px; margin-top: 16px; padding: 12px 16px; border-radius: 8px; font-size: 14px; }
    .adeudo-banner.al-corriente { background: #ecfdf5; color: #065f46; border: 1px solid #a7f3d0; }
    .adeudo-banner.moroso { background: #fef2f2; color: #991b1b; border: 1px solid #fecaca; }
    .metric-saldo { background: #eff6ff; border: 1px solid #bfdbfe; }
    .metric-value { display: block; font-size: 24px; font-weight: 700; color: #1e293b; }
    .metric-value.positive { color: #16a34a; }
    .metric-value.negative { color: #dc2626; }
    .metric-label { display: block; font-size: 12px; color: #6b7280; margin-top: 4px; }
    .movimientos-card { margin-top: 16px; }
    .filters-bar { display: flex; gap: 12px; margin-bottom: 16px; }
    .filter-field { min-width: 140px; }
    .full-width { width: 100%; }
    .tipo-badge { padding: 2px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; }
    .tipo-ABONO { background: #dcfce7; color: #166534; }
    .tipo-CARGO { background: #fef2f2; color: #991b1b; }
    .tipo-PAGO_RENTA { background: #eef2ff; color: #3730a3; }
    .amount-positive { color: #16a34a; font-weight: 600; }
    .amount-negative { color: #dc2626; font-weight: 600; }
    .ref-badge { background: #f1f5f9; padding: 2px 6px; border-radius: 4px; font-size: 11px; }
    .empty-state { text-align: center; padding: 32px; color: #9ca3af; }
  `],
})
export class EstadoCuentaDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private estadoCuentaService = inject(EstadoCuentaService);
  private snackBar = inject(MatSnackBar);

  tenantId = '';
  resumen: TenantEdoCuentaResumen | null = null;
  loading = true;

  movimientos: MovimientoEdoCuenta[] = [];
  totalMovimientos = 0;
  currentPage = 1;
  pageSize = 20;
  movLoading = false;
  filterTipo = '';
  filterDesde = '';
  filterHasta = '';

  columns = ['tipo', 'concepto', 'monto', 'saldoResultante', 'referencia', 'fecha'];

  tipoIcono(tipo: string): string {
    if (tipo === 'ABONO') return '📥';
    if (tipo === 'PAGO_RENTA') return '🧾';
    return '📤';
  }

  tipoLabel(tipo: string): string {
    if (tipo === 'PAGO_RENTA') return 'PAGO RENTA';
    return tipo;
  }

  ngOnInit(): void {
    this.tenantId = this.route.snapshot.paramMap.get('tenantId') || '';
    if (this.tenantId) {
      this.loadResumen();
      this.loadMovimientos();
    }
  }

  loadResumen(): void {
    this.loading = true;
    this.estadoCuentaService.getResumen(this.tenantId).subscribe({
      next: (r) => {
        this.resumen = r;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.snackBar.open(err?.error?.message || 'Error cargando resumen', 'Cerrar', { duration: 4000 });
      },
    });
  }

  applyFilters(): void {
    this.currentPage = 1;
    this.loadMovimientos();
  }

  clearFilters(): void {
    this.filterTipo = '';
    this.filterDesde = '';
    this.filterHasta = '';
    this.currentPage = 1;
    this.loadMovimientos();
  }

  loadMovimientos(): void {
    this.movLoading = true;
    this.estadoCuentaService.getMovimientos(this.tenantId, {
      tipo: this.filterTipo || undefined,
      desde: this.filterDesde || undefined,
      hasta: this.filterHasta || undefined,
      page: this.currentPage,
      pageSize: this.pageSize,
    }).subscribe({
      next: (result) => {
        this.movimientos = result.data;
        this.totalMovimientos = result.total;
        this.movLoading = false;
      },
      error: () => {
        this.movLoading = false;
        this.snackBar.open('Error cargando movimientos', 'Cerrar', { duration: 3000 });
      },
    });
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex + 1;
    this.pageSize = event.pageSize;
    this.loadMovimientos();
  }
}

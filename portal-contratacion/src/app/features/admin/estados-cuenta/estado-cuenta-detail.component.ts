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

            <!-- Resumen del mes: responde de un vistazo si la renta está cubierta. -->
            <div class="mes-resumen" [class.al-corriente]="resumen.mesPagado" [class.moroso]="!resumen.mesPagado">
              <div class="mes-resumen-header">
                <mat-icon>{{ resumen.mesPagado ? 'check_circle' : 'warning' }}</mat-icon>
                <span class="mes-titulo">
                  {{ periodoTitulo(resumen.periodoMes) }}
                  <strong class="mes-estado">{{ resumen.mesPagado ? 'PAGADO' : 'PENDIENTE' }}</strong>
                </span>
              </div>
              <div class="mes-resumen-lineas">
                <div class="linea">
                  <span class="linea-label">Renta del mes</span>
                  <span class="linea-valor">{{ resumen.mensualidad | currency:'MXN':'symbol':'1.2-2' }}</span>
                </div>
                @if (!resumen.mesPagado) {
                  <div class="linea">
                    <span class="linea-label">Pendiente por pagar</span>
                    <span class="linea-valor negative">{{ resumen.adeudoMes | currency:'MXN':'symbol':'1.2-2' }}</span>
                  </div>
                }
                @if (resumen.saldoActual > 0) {
                  <div class="linea">
                    <span class="linea-label">Saldo a favor</span>
                    <span class="linea-valor positive">
                      {{ resumen.saldoActual | currency:'MXN':'symbol':'1.2-2' }}
                      <small>· se aplica al próximo mes</small>
                    </span>
                  </div>
                }
                @if (resumen.servicioPagadoHasta) {
                  <div class="linea">
                    <span class="linea-label">Servicio cubierto hasta</span>
                    <span class="linea-valor">{{ resumen.servicioPagadoHasta }}</span>
                  </div>
                }
              </div>
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
                  <mat-option value="ABONO">Pagos recibidos</mat-option>
                  <mat-option value="CARGO">Cargos (renta / servicios)</mat-option>
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
                <td mat-cell *matCellDef="let m">
                  <span class="concepto-text">{{ conceptoLegible(m) }}</span>
                </td>
              </ng-container>

              <!-- Cargo: monto cobrado. Incluye CARGO (variable: almacenamiento,
                   excedentes) y PAGO_RENTA (renta aplicada desde fondos). El detalle
                   del tipo se distingue en la columna Tipo. -->
              <ng-container matColumnDef="cargo">
                <th mat-header-cell *matHeaderCellDef class="col-money">Cargo</th>
                <td mat-cell *matCellDef="let m" class="col-money">
                  @if (esCargo(m.tipo)) {
                    <span class="amount-negative">− {{ montoAbs(m.monto) | currency:'MXN':'symbol':'1.2-2' }}</span>
                  } @else {
                    <span class="amount-empty">—</span>
                  }
                </td>
              </ng-container>

              <!-- Abono: lo que el cliente paga o su saldo a favor recibido. -->
              <ng-container matColumnDef="abono">
                <th mat-header-cell *matHeaderCellDef class="col-money">Abono</th>
                <td mat-cell *matCellDef="let m" class="col-money">
                  @if (m.tipo === 'ABONO') {
                    <span class="amount-positive">+ {{ m.monto | currency:'MXN':'symbol':'1.2-2' }}</span>
                  } @else {
                    <span class="amount-empty">—</span>
                  }
                </td>
              </ng-container>

              <ng-container matColumnDef="saldoResultante">
                <th mat-header-cell *matHeaderCellDef class="col-money">Saldo a favor</th>
                <td mat-cell *matCellDef="let m" class="col-money">
                  <span [class.saldo-favor]="m.saldoResultante > 0">
                    {{ m.saldoResultante | currency:'MXN':'symbol':'1.2-2' }}
                  </span>
                </td>
              </ng-container>

              <ng-container matColumnDef="referencia">
                <th mat-header-cell *matHeaderCellDef>Referencia</th>
                <td mat-cell *matCellDef="let m">
                  <code class="ref-badge" [title]="m.claveRastreo || m.referencia || ''">{{ refCorta(m) }}</code>
                </td>
              </ng-container>

              <ng-container matColumnDef="fecha">
                <th mat-header-cell *matHeaderCellDef>Fecha</th>
                <td mat-cell *matCellDef="let m">{{ m.registradoEn | date:'dd/MM/yyyy HH:mm':'-0600' }}</td>
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
    .mes-resumen { margin-top: 16px; padding: 14px 16px; border-radius: 10px; font-size: 14px; }
    .mes-resumen.al-corriente { background: #ecfdf5; border: 1px solid #a7f3d0; }
    .mes-resumen.moroso { background: #fef2f2; border: 1px solid #fecaca; }
    .mes-resumen-header { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
    .mes-resumen.al-corriente .mes-resumen-header { color: #065f46; }
    .mes-resumen.moroso .mes-resumen-header { color: #991b1b; }
    .mes-titulo { font-size: 15px; }
    .mes-estado { margin-left: 8px; padding: 2px 8px; border-radius: 6px; font-size: 12px; letter-spacing: .04em; }
    .mes-resumen.al-corriente .mes-estado { background: #16a34a; color: #fff; }
    .mes-resumen.moroso .mes-estado { background: #dc2626; color: #fff; }
    .mes-resumen-lineas { display: flex; flex-direction: column; gap: 6px; }
    .linea { display: flex; justify-content: space-between; align-items: baseline; padding: 4px 0; border-top: 1px dashed rgba(0,0,0,.08); }
    .linea:first-child { border-top: none; }
    .linea-label { color: #475569; }
    .linea-valor { font-weight: 700; color: #1e293b; }
    .linea-valor.positive { color: #16a34a; }
    .linea-valor.negative { color: #dc2626; }
    .linea-valor small { font-weight: 400; color: #64748b; margin-left: 4px; }
    .metric-saldo { background: #eff6ff; border: 1px solid #bfdbfe; }
    .metric-value { display: block; font-size: 24px; font-weight: 700; color: #1e293b; }
    .metric-value.positive { color: #16a34a; }
    .metric-value.negative { color: #dc2626; }
    .metric-label { display: block; font-size: 12px; color: #6b7280; margin-top: 4px; }
    .movimientos-card { margin-top: 16px; }
    .filters-bar { display: flex; gap: 12px; margin-bottom: 16px; }
    .filter-field { min-width: 140px; }
    .full-width { width: 100%; }
    .tipo-badge { display: inline-block; padding: 3px 10px; border-radius: 12px; font-size: 11px; font-weight: 600; white-space: nowrap; }
    .tipo-ABONO { background: #dcfce7; color: #166534; }
    .tipo-CARGO { background: #fef2f2; color: #991b1b; }
    .tipo-PAGO_RENTA { background: #eef2ff; color: #3730a3; }
    .concepto-text { color: #1e293b; }
    .col-money { text-align: right; }
    td.col-money, th.col-money { text-align: right; padding-right: 16px; }
    .amount-positive { color: #16a34a; font-weight: 700; }
    .amount-negative { color: #dc2626; font-weight: 700; }
    .amount-empty { color: #cbd5e1; }
    .saldo-favor { color: #16a34a; font-weight: 700; }
    .ref-badge { background: #f1f5f9; padding: 2px 6px; border-radius: 4px; font-size: 11px; cursor: help; }
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

  columns = ['fecha', 'tipo', 'concepto', 'cargo', 'abono', 'saldoResultante', 'referencia'];

  /** Un movimiento es "cargo" (salida de dinero) si es CARGO variable o PAGO_RENTA. */
  esCargo(tipo: string): boolean {
    return tipo === 'CARGO' || tipo === 'PAGO_RENTA';
  }

  /** Monto siempre en positivo para mostrarlo en la columna (el signo lo da la columna). */
  montoAbs(monto: number): number {
    return Math.abs(monto ?? 0);
  }

  tipoIcono(tipo: string): string {
    if (tipo === 'ABONO') return '💵';
    return '🧾';
  }

  /**
   * Etiqueta de tipo. Se conservan los tres tipos distintos en la columna Tipo:
   * - ABONO       → "Pago recibido" (dinero que entra a favor del tenant)
   * - PAGO_RENTA  → "PAGO_RENTA" (renta aplicada desde fondos; conserva su etiqueta)
   * - CARGO       → "Cargo" (cobro variable: almacenamiento, excedentes)
   */
  tipoLabel(tipo: string): string {
    if (tipo === 'ABONO') return 'Pago recibido';
    if (tipo === 'CARGO') return 'Cargo';
    return tipo;
  }

  /**
   * Reescribe el concepto en lenguaje claro para el cliente. Normaliza los
   * conceptos crudos ("Pago Renta mensualidad CRM - 2026-09-01", "Saldo a favor
   * (excedente de pago)") a frases legibles y consistentes.
   */
  conceptoLegible(m: MovimientoEdoCuenta): string {
    const raw = (m.concepto || '').toLowerCase();

    // Renta mensual: mostrar el mes de forma legible.
    if (raw.includes('renta')) {
      const periodo = this.periodoLegible(m.periodoMes) || this.periodoDesdeConcepto(m.concepto);
      return periodo ? `Renta mensual CRM · ${periodo}` : 'Renta mensual CRM';
    }

    // Excedente / saldo a favor.
    if (raw.includes('excedente') || raw.includes('saldo a favor')) {
      return 'Saldo a favor (excedente de pago)';
    }

    if (raw.includes('almacen')) {
      return 'Cargo por almacenamiento';
    }

    // Pago recibido genérico.
    if (m.tipo === 'ABONO') {
      return m.concepto || 'Pago recibido';
    }

    return m.concepto || '—';
  }

  /** Convierte "2026-09" o "2026-09-01" a "Septiembre 2026". */
  private periodoLegible(periodo: string | null): string | null {
    if (!periodo) return null;
    const match = periodo.match(/^(\d{4})-(\d{2})/);
    if (!match) return null;
    const [, anio, mes] = match;
    const meses = [
      'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
      'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre',
    ];
    const idx = parseInt(mes, 10) - 1;
    if (idx < 0 || idx > 11) return null;
    return `${meses[idx]} ${anio}`;
  }

  /** Extrae un período tipo "2026-09-01" del texto del concepto, si existe. */
  private periodoDesdeConcepto(concepto: string): string | null {
    const match = (concepto || '').match(/(\d{4}-\d{2})/);
    return match ? this.periodoLegible(match[1]) : null;
  }

  /** Título del período para la tarjeta de resumen, ej. "Septiembre 2026". */
  periodoTitulo(periodo: string | null): string {
    return this.periodoLegible(periodo) || periodo || 'Mes en curso';
  }

  /** Referencia colapsada: "#c497" (últimos 4) o "MANUAL". El valor completo va en el tooltip. */
  refCorta(m: MovimientoEdoCuenta): string {
    const ref = m.claveRastreo || m.referencia;
    if (!ref) return '—';
    if (ref.startsWith('MANUAL')) return 'Manual';
    const limpio = ref.replace(/[^a-zA-Z0-9]/g, '');
    return limpio.length > 4 ? `#${limpio.slice(-4)}` : ref;
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

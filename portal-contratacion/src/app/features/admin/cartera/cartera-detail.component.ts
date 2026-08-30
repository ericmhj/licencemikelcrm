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
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CarteraService } from '../../../core/services/cartera.service';
import { TenantBalance, LedgerEntry, PaginatedLedger } from '../../../core/models/cartera.model';
import { CarteraAdjustDialogComponent } from './cartera-adjust-dialog.component';

@Component({
  selector: 'app-cartera-detail',
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
    MatSelectModule,
    MatInputModule,
    MatChipsModule,
    MatDialogModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
  ],
  template: `
    <div class="detail-container">
      <!-- Back button -->
      <a mat-button routerLink="/admin/cartera" class="back-btn">
        <mat-icon>arrow_back</mat-icon> Volver a búsqueda
      </a>

      @if (loading) {
        <div class="loading-state">
          <mat-spinner diameter="40"></mat-spinner>
          <p>Cargando información del tenant...</p>
        </div>
      }

      @if (balance && !loading) {
        <!-- Balance card -->
        <mat-card class="balance-card">
          <mat-card-header>
            <mat-card-title>
              <mat-icon>business</mat-icon>
              {{ balance.nombre }}
              <span class="slug-label">({{ balance.slug }})</span>
            </mat-card-title>
            <mat-card-subtitle>
              Plan: <strong>{{ balance.plan }}</strong> · Estado:
              <span [class]="'estado-inline estado-' + balance.estado">{{ balance.estado }}</span>
            </mat-card-subtitle>
          </mat-card-header>

          <mat-card-content>
            <div class="metrics-grid">
              <div class="metric">
                <span class="metric-value">{{ balance.saldoCreditos | number:'1.2-2' }}</span>
                <span class="metric-label">Saldo disponible (cr)</span>
              </div>
              <div class="metric">
                <span class="metric-value">{{ balance.creditosTotalesAdquiridos }}</span>
                <span class="metric-label">Total adquiridos</span>
              </div>
              <div class="metric">
                <span class="metric-value">{{ balance.creditosConsumidos }}</span>
                <span class="metric-label">Consumidos</span>
              </div>
              <div class="metric metric-sync">
                <span class="metric-value-small">{{ balance.ultimoSync | date:'medium' }}</span>
                <span class="metric-label">Último sync</span>
              </div>
            </div>

            <div class="actions-bar">
              <button mat-raised-button color="primary" (click)="openAdjustDialog()">
                <mat-icon>tune</mat-icon> Ajustar saldo
              </button>
              <button mat-button (click)="refreshBalance()">
                <mat-icon>refresh</mat-icon> Actualizar
              </button>
            </div>
          </mat-card-content>
        </mat-card>

        <!-- History card -->
        <mat-card class="history-card">
          <mat-card-header>
            <mat-card-title>Historial de movimientos</mat-card-title>
          </mat-card-header>

          <mat-card-content>
            <!-- Filters -->
            <div class="filters-bar">
              <mat-form-field appearance="outline" class="filter-field">
                <mat-label>Tipo</mat-label>
                <mat-select [(ngModel)]="filterTipo" (selectionChange)="loadHistory()">
                  <mat-option value="">Todos</mat-option>
                  <mat-option value="consumo">Consumo</mat-option>
                  <mat-option value="recarga">Recarga</mat-option>
                  <mat-option value="bonus">Bonus</mat-option>
                  <mat-option value="ajuste">Ajuste</mat-option>
                  <mat-option value="compensacion">Compensación</mat-option>
                  <mat-option value="excedente">Excedente</mat-option>
                </mat-select>
              </mat-form-field>

              <mat-form-field appearance="outline" class="filter-field">
                <mat-label>Desde</mat-label>
                <input matInput type="date" [(ngModel)]="filterDesde" (change)="loadHistory()">
              </mat-form-field>

              <mat-form-field appearance="outline" class="filter-field">
                <mat-label>Hasta</mat-label>
                <input matInput type="date" [(ngModel)]="filterHasta" (change)="loadHistory()">
              </mat-form-field>
            </div>

            <!-- Table -->
            <table mat-table [dataSource]="ledgerEntries" class="full-width">
              <ng-container matColumnDef="tipo">
                <th mat-header-cell *matHeaderCellDef>Tipo</th>
                <td mat-cell *matCellDef="let e">
                  <span [class]="'tipo-badge tipo-' + e.tipo">{{ tipoIcon(e.tipo) }} {{ e.tipo }}</span>
                </td>
              </ng-container>

              <ng-container matColumnDef="concepto">
                <th mat-header-cell *matHeaderCellDef>Concepto</th>
                <td mat-cell *matCellDef="let e">{{ e.concepto }}</td>
              </ng-container>

              <ng-container matColumnDef="cantidad">
                <th mat-header-cell *matHeaderCellDef>Cantidad</th>
                <td mat-cell *matCellDef="let e">
                  <span [class]="e.cantidad >= 0 ? 'amount-positive' : 'amount-negative'">
                    {{ e.cantidad >= 0 ? '+' : '' }}{{ e.cantidad | number:'1.2-2' }}
                  </span>
                </td>
              </ng-container>

              <ng-container matColumnDef="saldoResultante">
                <th mat-header-cell *matHeaderCellDef>Saldo</th>
                <td mat-cell *matCellDef="let e">{{ e.saldoResultante | number:'1.2-2' }}</td>
              </ng-container>

              <ng-container matColumnDef="createdAt">
                <th mat-header-cell *matHeaderCellDef>Fecha</th>
                <td mat-cell *matCellDef="let e">{{ e.createdAt | date:'short' }}</td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="ledgerColumns"></tr>
              <tr mat-row *matRowDef="let row; columns: ledgerColumns;"></tr>
            </table>

            @if (ledgerEntries.length === 0 && !historyLoading) {
              <div class="empty-history">
                <p>Sin movimientos registrados</p>
              </div>
            }

            <mat-paginator
              [length]="totalEntries"
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
    .balance-card { margin-bottom: 24px; }
    .slug-label { font-size: 14px; color: #6b7280; font-weight: 400; margin-left: 8px; }
    .estado-inline { padding: 2px 8px; border-radius: 8px; font-size: 12px; font-weight: 600; }
    .estado-active, .estado-ACTIVE { background: #dcfce7; color: #166534; }
    .estado-suspended, .estado-SUSPENDED { background: #fef2f2; color: #991b1b; }

    .metrics-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin: 16px 0; }
    .metric { background: #f8fafc; border-radius: 8px; padding: 16px; text-align: center; }
    .metric-value { display: block; font-size: 28px; font-weight: 700; color: #1e293b; }
    .metric-value-small { display: block; font-size: 14px; font-weight: 500; color: #475569; }
    .metric-label { display: block; font-size: 12px; color: #6b7280; margin-top: 4px; }

    .actions-bar { display: flex; gap: 12px; margin-top: 16px; padding-top: 16px; border-top: 1px solid #e5e7eb; }
    .history-card { margin-top: 16px; }
    .filters-bar { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
    .filter-field { min-width: 140px; }
    .full-width { width: 100%; }

    .tipo-badge { padding: 2px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; }
    .tipo-consumo { background: #fef2f2; color: #991b1b; }
    .tipo-recarga { background: #dcfce7; color: #166534; }
    .tipo-bonus { background: #ede9fe; color: #5b21b6; }
    .tipo-ajuste { background: #fef9c3; color: #854d0e; }
    .tipo-compensacion { background: #e0f2fe; color: #075985; }
    .tipo-excedente { background: #fef2f2; color: #991b1b; }

    .amount-positive { color: #16a34a; font-weight: 600; }
    .amount-negative { color: #dc2626; font-weight: 600; }
    .empty-history { text-align: center; padding: 32px; color: #9ca3af; }
  `],
})
export class CarteraDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private carteraService = inject(CarteraService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);

  tenantId = '';
  balance: TenantBalance | null = null;
  loading = true;

  // History
  ledgerEntries: LedgerEntry[] = [];
  totalEntries = 0;
  currentPage = 1;
  pageSize = 20;
  historyLoading = false;

  // Filters
  filterTipo = '';
  filterDesde = '';
  filterHasta = '';

  ledgerColumns = ['tipo', 'concepto', 'cantidad', 'saldoResultante', 'createdAt'];

  ngOnInit(): void {
    this.tenantId = this.route.snapshot.paramMap.get('tenantId') || '';
    if (this.tenantId) {
      this.refreshBalance();
      this.loadHistory();
    }
  }

  refreshBalance(): void {
    this.loading = true;
    this.carteraService.getBalance(this.tenantId).subscribe({
      next: (b) => {
        this.balance = b;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.snackBar.open(err?.error?.message || 'Error cargando balance', 'Cerrar', { duration: 4000 });
      },
    });
  }

  loadHistory(): void {
    this.historyLoading = true;
    this.carteraService
      .getHistory(this.tenantId, {
        tipo: this.filterTipo || undefined,
        desde: this.filterDesde || undefined,
        hasta: this.filterHasta || undefined,
        page: this.currentPage,
        pageSize: this.pageSize,
      })
      .subscribe({
        next: (result) => {
          this.ledgerEntries = result.data;
          this.totalEntries = result.total;
          this.historyLoading = false;
        },
        error: () => {
          this.historyLoading = false;
          this.snackBar.open('Error cargando historial', 'Cerrar', { duration: 3000 });
        },
      });
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex + 1;
    this.pageSize = event.pageSize;
    this.loadHistory();
  }

  openAdjustDialog(): void {
    const dialogRef = this.dialog.open(CarteraAdjustDialogComponent, {
      width: '500px',
      disableClose: true,
      data: {
        tenantId: this.tenantId,
        tenantNombre: this.balance?.nombre || '',
        saldoActual: this.balance?.saldoCreditos || 0,
      },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result === 'success') {
        this.refreshBalance();
        this.loadHistory();
        this.snackBar.open('Ajuste aplicado exitosamente', 'OK', { duration: 4000 });
      }
    });
  }

  tipoIcon(tipo: string): string {
    const icons: Record<string, string> = {
      consumo: '📤',
      recarga: '📥',
      bonus: '🎁',
      ajuste: '⚙️',
      compensacion: '↩️',
      excedente: '⚠️',
    };
    return icons[tipo] || '•';
  }
}

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
import { CobroAdminService, ClabeTenant, TipoCobro } from '../../../core/services/cobro-admin.service';
import { TenantSummary } from '../../../core/models/cartera.model';

@Component({
  selector: 'app-cobros',
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
        <mat-card-title>Cobros</mat-card-title>
        <mat-card-subtitle>Administra CLABEs y registra pagos mensuales manuales</mat-card-subtitle>
      </mat-card-header>

      <mat-card-content>
        @if (loadingTenants()) {
          <div class="center"><mat-spinner diameter="40"></mat-spinner></div>
        } @else {
          <mat-form-field appearance="outline" class="search-field">
            <mat-label>Selecciona un tenant</mat-label>
            <mat-select [(ngModel)]="selectedTenantId" (ngModelChange)="onSelectTenant($event)">
              @for (t of tenants(); track t.id) {
                <mat-option [value]="t.id">{{ t.nombre }} ({{ t.slug }})</mat-option>
              }
            </mat-select>
            <mat-icon matPrefix>search</mat-icon>
          </mat-form-field>

          @if (selectedTenantId) {
            <!-- ─── CLABEs ─────────────────────────────────────────── -->
            <section class="panel">
              <h3>CLABEs registradas</h3>

              @if (loadingClabes()) {
                <div class="center"><mat-spinner diameter="28"></mat-spinner></div>
              } @else {
                <table mat-table [dataSource]="clabes()" class="full-width">
                  <ng-container matColumnDef="clabe">
                    <th mat-header-cell *matHeaderCellDef>CLABE</th>
                    <td mat-cell *matCellDef="let c">{{ c.clabe }}</td>
                  </ng-container>
                  <ng-container matColumnDef="tipo">
                    <th mat-header-cell *matHeaderCellDef>Tipo</th>
                    <td mat-cell *matCellDef="let c">
                      <mat-chip>{{ c.tipo === 'FIJO_MENSUAL' ? 'Fijo mensual' : 'Prepago variable' }}</mat-chip>
                    </td>
                  </ng-container>
                  <ng-container matColumnDef="psp">
                    <th mat-header-cell *matHeaderCellDef>PSP</th>
                    <td mat-cell *matCellDef="let c">{{ c.psp }}</td>
                  </ng-container>
                  <ng-container matColumnDef="acciones">
                    <th mat-header-cell *matHeaderCellDef></th>
                    <td mat-cell *matCellDef="let c">
                      <button mat-icon-button color="warn" (click)="deactivate(c)" matTooltip="Desactivar">
                        <mat-icon>delete</mat-icon>
                      </button>
                    </td>
                  </ng-container>
                  <tr mat-header-row *matHeaderRowDef="clabeColumns"></tr>
                  <tr mat-row *matRowDef="let row; columns: clabeColumns;"></tr>
                </table>

                @if (clabes().length === 0) {
                  <p class="empty">Este tenant no tiene CLABEs activas.</p>
                }
              }

              <!-- Alta de CLABE: solo si el tenant no tiene una CLABE activa. -->
              @if (clabes().length === 0) {
                <p class="hint">
                  Cada tenant tiene una única CLABE. Registra la CLABE de este tenant.
                </p>
                <div class="form-row">
                  <mat-form-field appearance="outline">
                    <mat-label>Nueva CLABE (18 dígitos)</mat-label>
                    <input matInput [(ngModel)]="nuevaClabe" maxlength="18" placeholder="000000000000000000" />
                  </mat-form-field>
                  <button mat-raised-button color="primary" [disabled]="savingClabe()" (click)="addClabe()">
                    <mat-icon>add</mat-icon> Registrar CLABE
                  </button>
                </div>
              } @else {
                <p class="hint">
                  Este tenant ya tiene una CLABE registrada. Para cambiarla, desactiva la actual primero.
                </p>
              }
            </section>

            <!-- ─── Pago manual ─────────────────────────────────────── -->
            <section class="panel">
              <h3>Registrar pago</h3>
              <p class="hint">
                Aplica un pago al tenant (equivalente a un SPEI recibido a su CLABE).
                Se aplica primero a la renta mensual pendiente (meses completos) y el
                excedente se abona como saldo a favor. Requiere CLABE y plan asignado.
              </p>
              <div class="form-row">
                <mat-form-field appearance="outline">
                  <mat-label>Monto (MXN)</mat-label>
                  <input matInput type="number" [(ngModel)]="montoPago" min="0" step="0.01" />
                </mat-form-field>
                <mat-form-field appearance="outline" class="grow">
                  <mat-label>Referencia (opcional)</mat-label>
                  <input matInput [(ngModel)]="referenciaPago" placeholder="Pago mensual manual" />
                </mat-form-field>
                <button mat-raised-button color="primary" [disabled]="savingPago() || !montoPago" (click)="registrarPago()">
                  <mat-icon>payments</mat-icon> Aplicar pago
                </button>
              </div>
            </section>
          }
        }
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .full-width { width: 100%; }
    .search-field { width: 100%; max-width: 480px; margin-bottom: 16px; }
    .center { display: flex; justify-content: center; padding: 24px; }
    .panel { margin-top: 24px; padding-top: 16px; border-top: 1px solid #e0e0e0; }
    .panel h3 { margin: 0 0 8px; color: #1976d2; }
    .hint { font-size: 12px; color: #6b7280; margin: 0 0 12px; }
    .empty { color: #6b7280; font-style: italic; padding: 12px 0; }
    .form-row { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; margin-top: 12px; }
    .form-row .grow { flex: 1; min-width: 220px; }
  `],
})
export class CobrosComponent implements OnInit {
  private carteraService = inject(CarteraService);
  private cobroService = inject(CobroAdminService);
  private snackBar = inject(MatSnackBar);

  tenants = signal<TenantSummary[]>([]);
  clabes = signal<ClabeTenant[]>([]);
  loadingTenants = signal(true);
  loadingClabes = signal(false);
  savingClabe = signal(false);
  savingPago = signal(false);

  selectedTenantId = '';
  nuevaClabe = '';
  nuevaClabeTipo: TipoCobro = 'FIJO_MENSUAL';
  montoPago: number | null = null;
  referenciaPago = '';

  clabeColumns = ['clabe', 'tipo', 'psp', 'acciones'];

  ngOnInit(): void {
    this.carteraService.listAllTenants().subscribe({
      next: (list) => {
        this.tenants.set(list);
        this.loadingTenants.set(false);
      },
      error: () => {
        this.snackBar.open('Error cargando tenants', 'Cerrar', { duration: 4000 });
        this.loadingTenants.set(false);
      },
    });
  }

  onSelectTenant(tenantId: string): void {
    this.selectedTenantId = tenantId;
    this.loadClabes();
  }

  private loadClabes(): void {
    if (!this.selectedTenantId) return;
    this.loadingClabes.set(true);
    this.cobroService.listClabes(this.selectedTenantId).subscribe({
      next: (list) => {
        this.clabes.set(list);
        this.loadingClabes.set(false);
      },
      error: () => {
        this.snackBar.open('Error cargando CLABEs', 'Cerrar', { duration: 4000 });
        this.loadingClabes.set(false);
      },
    });
  }

  addClabe(): void {
    const clabe = (this.nuevaClabe || '').trim();
    if (!/^\d{18}$/.test(clabe)) {
      this.snackBar.open('La CLABE debe tener exactamente 18 dígitos', 'Cerrar', { duration: 3000 });
      return;
    }
    this.savingClabe.set(true);
    // Una sola CLABE por tenant; el tipo se conserva como metadato (FIJO_MENSUAL).
    this.cobroService.createClabe(this.selectedTenantId, { clabe, tipo: 'FIJO_MENSUAL', psp: 'MANUAL' }).subscribe({
      next: () => {
        this.savingClabe.set(false);
        this.nuevaClabe = '';
        this.snackBar.open('CLABE registrada', 'OK', { duration: 3000 });
        this.loadClabes();
      },
      error: (e) => {
        this.savingClabe.set(false);
        this.snackBar.open(e?.error?.message || 'Error al registrar CLABE', 'Cerrar', { duration: 4000 });
      },
    });
  }

  deactivate(c: ClabeTenant): void {
    if (!confirm(`¿Desactivar la CLABE ${c.clabe}?`)) return;
    this.cobroService.deactivateClabe(this.selectedTenantId, c.id).subscribe({
      next: () => {
        this.snackBar.open('CLABE desactivada', 'OK', { duration: 3000 });
        this.loadClabes();
      },
      error: () => this.snackBar.open('Error al desactivar', 'Cerrar', { duration: 4000 }),
    });
  }

  registrarPago(): void {
    if (!this.montoPago || this.montoPago <= 0) {
      this.snackBar.open('Ingresa un monto válido', 'Cerrar', { duration: 3000 });
      return;
    }
    this.savingPago.set(true);
    this.cobroService
      .registrarPagoMensual(this.selectedTenantId, this.montoPago, this.referenciaPago || undefined)
      .subscribe({
        next: (res) => {
          this.savingPago.set(false);
          if (res.estado === 'APLICADO') {
            this.snackBar.open(
              `Pago aplicado. Créditos otorgados: ${res.creditosOtorgados ?? 0}`,
              'OK',
              { duration: 4000 },
            );
            this.montoPago = null;
            this.referenciaPago = '';
          } else {
            this.snackBar.open(`Pago rechazado (estado: ${res.estado})`, 'Cerrar', { duration: 5000 });
          }
        },
        error: (e) => {
          this.savingPago.set(false);
          this.snackBar.open(e?.error?.message || 'Error al aplicar el pago', 'Cerrar', { duration: 5000 });
        },
      });
  }
}

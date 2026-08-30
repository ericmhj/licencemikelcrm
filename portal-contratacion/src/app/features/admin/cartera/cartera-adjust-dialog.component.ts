import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CarteraService } from '../../../core/services/cartera.service';
import { AdjustCreditRequest } from '../../../core/models/cartera.model';

interface DialogData {
  tenantId: string;
  tenantNombre: string;
  saldoActual: number;
}

@Component({
  selector: 'app-cartera-adjust-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatRadioModule,
    MatCheckboxModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon class="warn-icon">warning</mat-icon>
      Ajuste de Saldo — {{ data.tenantNombre }}
    </h2>

    <mat-dialog-content>
      <div class="current-balance">
        <span class="label">Saldo actual:</span>
        <span class="value">{{ data.saldoActual | number:'1.2-2' }} créditos</span>
      </div>

      <div class="saldo-favor">
        <span class="label">Total saldo a favor después del abono:</span>
        <span class="value" [class.exceeded]="saldoResultante > 25000">{{ saldoResultante | number:'1.2-2' }} créditos</span>
      </div>

      <!-- Operation type -->
      <div class="field-group">
        <label class="field-label">Tipo de operación:</label>
        <mat-radio-group [(ngModel)]="tipoOperacion" class="radio-group">
          <mat-radio-button value="recarga">
            <mat-icon class="radio-icon">add_circle</mat-icon>
            Agregar créditos (recarga manual)
          </mat-radio-button>
        </mat-radio-group>
      </div>

      <!-- Amount -->
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Cantidad (créditos)</mat-label>
        <input matInput
               type="number"
               [(ngModel)]="cantidad"
               [min]="0.5"
               [max]="22000"
               step="0.5"
               placeholder="Ej: 50"
               (keydown)="onlyNumbers($event)">
        <mat-hint>Mínimo: 0.5 — Máximo: 22,000 por operación</mat-hint>
      </mat-form-field>

      <!-- Reason (mandatory) -->
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Número de referencia (obligatorio)</mat-label>
        <input matInput
               [(ngModel)]="referencia"
               maxlength="10"
               placeholder="Ej: REF-12345">
        <mat-hint>{{ referencia.length }}/10 caracteres</mat-hint>
      </mat-form-field>

      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Número de autorización (obligatorio)</mat-label>
        <input matInput
               [(ngModel)]="autorizacion"
               maxlength="20"
               placeholder="Ej: AUTH-98765">
        <mat-hint>{{ autorizacion.length }}/20 caracteres</mat-hint>
      </mat-form-field>

      <!-- Motivo del ajuste -->
      <div class="field-group">
        <label class="field-label">Motivo del ajuste (obligatorio, seleccione al menos uno):</label>
        <div class="checkbox-group">
          <mat-checkbox [(ngModel)]="motivoPlanMensual">Plan Mensual</mat-checkbox>
          <mat-checkbox [(ngModel)]="motivoPagoEstudio">Pago Estudio</mat-checkbox>
          <mat-checkbox [(ngModel)]="motivoAbonoPrepago">Abono Prepago</mat-checkbox>
        </div>
      </div>

      <!-- Preview -->
      @if (cantidad > 0 && tipoOperacion) {
        <div class="preview-box">
          <span class="preview-label">Saldo resultante:</span>
          <span class="preview-value" [class.negative]="saldoResultante < 0" [class.exceeded]="saldoResultante > 25000">
            {{ saldoResultante | number:'1.2-2' }} créditos
          </span>
          @if (saldoResultante < 0) {
            <p class="preview-error">⚠️ El saldo resultante sería negativo. No se permite.</p>
          }
          @if (saldoResultante > 25000) {
            <p class="preview-error">⚠️ La cartera no puede exceder 25,000 créditos.</p>
          }
        </div>
      }

      <!-- Confirmation checkbox -->
      <div class="confirmation-section">
        <mat-checkbox [(ngModel)]="confirmed" color="warn">
          Confirmo que esta operación es correcta y quedará registrada
          en el audit log con mi identificador de usuario.
        </mat-checkbox>
      </div>

      <!-- Error message -->
      @if (errorMessage) {
        <div class="error-box">
          <mat-icon>error</mat-icon> {{ errorMessage }}
        </div>
      }
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close [disabled]="submitting">Cancelar</button>
      <button mat-raised-button
              color="warn"
              (click)="submit()"
              [disabled]="!canSubmit || submitting">
        @if (submitting) {
          <mat-spinner diameter="18"></mat-spinner>
        } @else {
          <mat-icon>check_circle</mat-icon> Confirmar ajuste
        }
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .warn-icon { color: #f59e0b; vertical-align: middle; margin-right: 8px; }
    .current-balance { background: #f1f5f9; padding: 12px 16px; border-radius: 8px; margin-bottom: 12px; display: flex; justify-content: space-between; }
    .current-balance .label { color: #64748b; }
    .current-balance .value { font-weight: 700; font-size: 18px; color: #1e293b; }
    .saldo-favor { background: #dcfce7; padding: 12px 16px; border-radius: 8px; margin-bottom: 20px; display: flex; justify-content: space-between; }
    .saldo-favor .label { color: #166534; font-weight: 500; }
    .saldo-favor .value { font-weight: 700; font-size: 18px; color: #166534; }
    .saldo-favor .value.exceeded { color: #dc2626; }
    .field-group { margin-bottom: 20px; }
    .field-label { display: block; font-size: 14px; color: #374151; margin-bottom: 8px; font-weight: 500; }
    .radio-group { display: flex; flex-direction: column; gap: 8px; }
    .checkbox-group { display: flex; flex-direction: column; gap: 8px; }
    .radio-icon { font-size: 18px; vertical-align: middle; margin-right: 4px; }
    .full-width { width: 100%; margin-bottom: 16px; }
    .preview-box { background: #eff6ff; border: 1px solid #bfdbfe; border-radius: 8px; padding: 12px 16px; margin-bottom: 16px; }
    .preview-label { color: #1e40af; font-size: 13px; }
    .preview-value { display: block; font-size: 22px; font-weight: 700; color: #1e40af; }
    .preview-value.negative { color: #dc2626; }
    .preview-error { color: #dc2626; font-size: 12px; margin-top: 4px; }
    .confirmation-section { background: #fef9c3; padding: 12px; border-radius: 8px; margin: 16px 0; }
    .error-box { background: #fef2f2; color: #991b1b; padding: 10px 14px; border-radius: 6px; margin-top: 12px; display: flex; align-items: center; gap: 8px; font-size: 13px; }
  `],
})
export class CarteraAdjustDialogComponent {
  private dialogRef = inject(MatDialogRef<CarteraAdjustDialogComponent>);
  private carteraService = inject(CarteraService);
  private snackBar = inject(MatSnackBar);
  data: DialogData = inject(MAT_DIALOG_DATA);

  tipoOperacion: 'recarga' | 'ajuste' = 'recarga';
  cantidad = 0;
  motivo = '';
  referencia = '';
  autorizacion = '';
  motivoPlanMensual = false;
  motivoPagoEstudio = false;
  motivoAbonoPrepago = false;
  confirmed = false;
  submitting = false;
  errorMessage = '';

  get motivoSeleccionado(): string {
    const motivos: string[] = [];
    if (this.motivoPlanMensual) motivos.push('Plan Mensual');
    if (this.motivoPagoEstudio) motivos.push('Pago Estudio');
    if (this.motivoAbonoPrepago) motivos.push('Abono Prepago');
    return motivos.join(', ');
  }

  get saldoResultante(): number {
    return this.data.saldoActual + this.cantidad;
  }

  get canSubmit(): boolean {
    return (
      this.confirmed &&
      this.cantidad >= 0.5 &&
      this.cantidad <= 22000 &&
      this.referencia.trim().length >= 3 &&
      this.autorizacion.trim().length >= 3 &&
      this.motivoSeleccionado.length > 0 &&
      this.saldoResultante >= 0 &&
      this.saldoResultante <= 25000 &&
      !!this.tipoOperacion
    );
  }

  /** Solo permite dígitos, punto decimal, backspace, tab, flechas */
  onlyNumbers(event: KeyboardEvent): void {
    const allowed = ['Backspace', 'Tab', 'ArrowLeft', 'ArrowRight', 'Delete', 'Home', 'End'];
    if (allowed.includes(event.key)) return;
    if (event.key === '.' && !String(this.cantidad).includes('.')) return;
    if (!/^\d$/.test(event.key)) {
      event.preventDefault();
    }
  }

  submit(): void {
    if (!this.canSubmit) return;

    this.submitting = true;
    this.errorMessage = '';

    const request: AdjustCreditRequest = {
      operationId: crypto.randomUUID(),
      tipo: 'recarga',
      cantidad: Math.abs(this.cantidad),
      motivo: this.motivoSeleccionado,
      referencia: this.referencia.trim(),
      autorizacion: this.autorizacion.trim(),
    };

    this.carteraService.adjustCredits(this.data.tenantId, request).subscribe({
      next: (response) => {
        this.submitting = false;
        if (response.status === 'approved') {
          this.dialogRef.close('success');
        } else {
          this.errorMessage = response.message || 'Operación rechazada por el servidor';
        }
      },
      error: (err) => {
        this.submitting = false;
        if (err.status === 429) {
          this.errorMessage = 'Demasiadas operaciones. Espere un momento antes de intentar de nuevo.';
        } else if (err.status === 403) {
          this.errorMessage = 'No tiene permisos para realizar esta operación.';
        } else {
          this.errorMessage = err?.error?.message || 'Error de comunicación con el servidor';
        }
      },
    });
  }
}

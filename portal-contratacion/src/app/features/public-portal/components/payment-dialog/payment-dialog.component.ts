import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatDialogRef, MatDialogModule, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { environment } from '../../../../../environments/environment';

export interface PaymentDialogData {
  amount: number;
  description?: string;
}

export interface PaymentDialogResult {
  approved: boolean;
  transactionId?: string;
}

@Component({
  selector: 'app-payment-dialog',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatDialogModule,
    MatFormFieldModule, MatInputModule, MatButtonModule,
    MatIconModule, MatProgressSpinnerModule,
  ],
  template: `
    <h2 mat-dialog-title>Datos de Pago</h2>
    <mat-dialog-content class="space-y-4 min-w-[350px]">
      <mat-form-field class="w-full" appearance="outline">
        <mat-label>Número de tarjeta</mat-label>
        <input matInput [(ngModel)]="card.number" maxlength="19"
               placeholder="4242 4242 4242 4242" (input)="formatCardNumber($event)" />
        <mat-hint>16 dígitos</mat-hint>
      </mat-form-field>

      <mat-form-field class="w-full" appearance="outline">
        <mat-label>Nombre del titular</mat-label>
        <input matInput [(ngModel)]="card.holder" placeholder="JUAN PEREZ" />
      </mat-form-field>

      <div class="flex gap-3">
        <mat-form-field class="flex-1" appearance="outline">
          <mat-label>Expiración</mat-label>
          <input matInput [(ngModel)]="card.expiry" maxlength="5"
                 placeholder="MM/YY" (input)="formatExpiry($event)" />
        </mat-form-field>
        <mat-form-field class="w-24" appearance="outline">
          <mat-label>CVV</mat-label>
          <input matInput [(ngModel)]="card.cvv" maxlength="4"
                 type="password" placeholder="123" />
        </mat-form-field>
      </div>

      @if (error()) {
        <div class="p-3 bg-red-50 border border-red-200 rounded text-red-700 text-sm">
          {{ error() }}
        </div>
      }
    </mat-dialog-content>

    <mat-dialog-actions align="end" class="p-4">
      <button mat-stroked-button mat-dialog-close [disabled]="processing()">Cancelar</button>
      <button mat-raised-button color="primary"
              [disabled]="!isValid() || processing()"
              (click)="pay()">
        @if (processing()) { Procesando... } @else { Pagar }
      </button>
    </mat-dialog-actions>
  `,
})
export class PaymentDialogComponent {
  private readonly http = inject(HttpClient);
  private readonly dialogRef = inject(MatDialogRef<PaymentDialogComponent>);
  private readonly data: PaymentDialogData = inject(MAT_DIALOG_DATA);

  processing = signal(false);
  error = signal<string | null>(null);

  card = { number: '', holder: '', expiry: '', cvv: '' };

  formatCardNumber(event: Event): void {
    const input = event.target as HTMLInputElement;
    let value = input.value.replace(/\D/g, '');
    value = value.substring(0, 16);
    value = value.replace(/(\d{4})(?=\d)/g, '$1 ');
    this.card.number = value;
    input.value = value;
  }

  formatExpiry(event: Event): void {
    const input = event.target as HTMLInputElement;
    let value = input.value.replace(/\D/g, '');
    if (value.length >= 2) {
      value = value.substring(0, 2) + '/' + value.substring(2, 4);
    }
    this.card.expiry = value;
    input.value = value;
  }

  isValid(): boolean {
    const num = this.card.number.replace(/\s/g, '');
    return num.length >= 13 && num.length <= 19 &&
           this.passesLuhn(num) &&
           this.card.holder.trim().length > 0 &&
           /^\d{2}\/\d{2}$/.test(this.card.expiry) &&
           /^\d{3,4}$/.test(this.card.cvv);
  }

  async pay(): Promise<void> {
    this.processing.set(true);
    this.error.set(null);

    try {
      const response: any = await this.http.post(`${environment.apiUrl}/api/v1/payments/process`, {
        cardNumber: this.card.number.replace(/\s/g, ''),
        holderName: this.card.holder,
        expiryDate: this.card.expiry,
        cvv: this.card.cvv,
        amount: this.data.amount,
        currency: 'EUR',
        description: this.data.description || '',
      }).toPromise();

      if (response.status === 'APPROVED') {
        this.dialogRef.close({ approved: true, transactionId: response.transactionId } as PaymentDialogResult);
      } else {
        this.error.set(response.message || 'Pago rechazado');
        this.processing.set(false);
      }
    } catch (err: any) {
      this.error.set('Error de conexión. Intenta de nuevo.');
      this.processing.set(false);
    }
  }

  private passesLuhn(number: string): boolean {
    let sum = 0;
    let alternate = false;
    for (let i = number.length - 1; i >= 0; i--) {
      let n = parseInt(number[i], 10);
      if (alternate) { n *= 2; if (n > 9) n -= 9; }
      sum += n;
      alternate = !alternate;
    }
    return sum % 10 === 0;
  }
}

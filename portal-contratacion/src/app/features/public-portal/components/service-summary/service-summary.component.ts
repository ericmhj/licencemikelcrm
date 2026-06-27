import { Component, input, signal, inject, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { SubtotalItem, PaqueteCreditos } from '../../../../core/models/calculator.model';

@Component({
  selector: 'app-service-summary',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatButtonModule, MatSnackBarModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <mat-card>
      <mat-card-content class="p-4">
        <button
          mat-stroked-button
          class="w-full"
          (click)="toggleSummary()"
          [attr.aria-expanded]="showSummary()"
          aria-controls="summary-content"
        >
          <mat-icon>{{ showSummary() ? 'visibility_off' : 'visibility' }}</mat-icon>
          {{ showSummary() ? 'Ocultar resumen' : 'Ver resumen de servicios' }}
        </button>

        @if (showSummary()) {
          <div id="summary-content" class="mt-4">
            <textarea
              #summaryText
              readonly
              class="w-full h-48 p-3 border border-gray-300 rounded text-sm font-mono bg-gray-50 resize-none"
              [value]="generatedSummary()"
              aria-label="Resumen de servicios contratados"
            ></textarea>
            <button
              mat-raised-button
              color="primary"
              class="mt-3 w-full"
              (click)="copyToClipboard(summaryText)"
            >
              <mat-icon>content_copy</mat-icon>
              Copiar al portapapeles
            </button>
          </div>
        }
      </mat-card-content>
    </mat-card>
  `,
  styles: []
})
export class ServiceSummaryComponent {
  private readonly snackBar = inject(MatSnackBar);

  subtotales = input.required<SubtotalItem[]>();
  totalAnual = input.required<number>();
  cuotaMensual = input.required<number>();
  selectedPackage = input.required<PaqueteCreditos | null>();

  showSummary = signal(false);

  toggleSummary(): void {
    this.showSummary.update(v => !v);
  }

  generatedSummary(): string {
    const lines: string[] = [];
    const fecha = new Date();

    lines.push('=== RESUMEN DE SERVICIOS — Mikel CRM ===');
    lines.push(`Fecha: ${fecha.toLocaleDateString('es-ES')}`);
    lines.push('');
    lines.push('--- Módulos seleccionados ---');

    for (const item of this.subtotales()) {
      lines.push(`• ${item.nombre}: ${this.formatEur(item.precioMensual)}/mes (${this.formatEur(item.subtotalAnual)}/año)`);
    }

    lines.push('');

    const pkg = this.selectedPackage();
    if (pkg) {
      lines.push('--- Paquete de créditos ---');
      lines.push(`• ${pkg.nombre}: ${pkg.creditos} créditos + ${pkg.bonus} bonus — ${this.formatEur(pkg.precioAnual)}/año`);
      lines.push('');
    }

    lines.push('--- Totales ---');
    lines.push(`Total anual: ${this.formatEur(this.totalAnual())}`);
    lines.push(`Cuota mensual estimada: ${this.formatEur(this.cuotaMensual())}`);
    lines.push('');
    lines.push('--- Política de descuentos ---');
    lines.push('• 10% descuento: pago antes de la fecha límite');
    lines.push('• 3% descuento: pago en la fecha límite');
    lines.push('• 0% descuento: pago posterior a la fecha límite');

    return lines.join('\n');
  }

  async copyToClipboard(textarea: HTMLTextAreaElement): Promise<void> {
    const text = this.generatedSummary();
    try {
      if (navigator.clipboard && navigator.clipboard.writeText) {
        await navigator.clipboard.writeText(text);
        this.snackBar.open('Resumen copiado al portapapeles', 'OK', { duration: 3000 });
      } else {
        // Fallback: select all text
        textarea.select();
        textarea.setSelectionRange(0, textarea.value.length);
        this.snackBar.open('Texto seleccionado — usa Ctrl+C para copiar', 'OK', { duration: 3000 });
      }
    } catch {
      // Fallback: select all text
      textarea.select();
      textarea.setSelectionRange(0, textarea.value.length);
      this.snackBar.open('Texto seleccionado — usa Ctrl+C para copiar', 'OK', { duration: 3000 });
    }
  }

  private formatEur(value: number): string {
    return value.toLocaleString('es-ES', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' €';
  }
}

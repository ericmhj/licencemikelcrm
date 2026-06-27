import { Component, computed, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-error-handler',
  standalone: true,
  imports: [MatButtonModule],
  template: `
    <div class="p-4 border border-red-200 bg-red-50 rounded text-center">
      <p class="text-red-700">{{ errorMessage() }}</p>
      <button mat-stroked-button color="warn" class="mt-3" (click)="retry.emit()">Reintentar</button>
    </div>
  `,
})
export class ErrorHandlerComponent {
  error = input.required<string>();
  retry = output<void>();

  errorMessage = computed(() => {
    switch (this.error()) {
      case 'TIMEOUT': return 'Tiempo de espera agotado. Intenta de nuevo.';
      case 'SERVER_ERROR': return 'Error en el servidor. Intenta más tarde.';
      default: return 'Ocurrió un error inesperado.';
    }
  });
}

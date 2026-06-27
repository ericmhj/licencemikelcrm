import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-action-buttons',
  standalone: true,
  imports: [CommonModule, MatButtonModule],
  template: `
    <div class="flex flex-wrap gap-3">
      @if (isAdmin()) {
        <button mat-raised-button color="primary" class="min-h-[44px]">
          Comprar créditos adicionales
        </button>
        <button mat-raised-button color="primary" class="min-h-[44px]">
          Añadir módulo
        </button>
      }
      <button mat-raised-button class="min-h-[44px]">
        Ver historial de facturas
      </button>
    </div>
  `,
})
export class ActionButtonsComponent {
  private readonly authService = inject(AuthService);

  readonly isAdmin = computed(() => this.authService.getUserRole() === 'ADMIN_CUENTA');
}

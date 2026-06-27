import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-contract-confirmation',
  standalone: true,
  imports: [CommonModule, RouterLink, MatCardModule, MatButtonModule, MatIconModule],
  template: `
    <div class="min-h-screen bg-[#F5F5F5] flex items-center justify-center p-4">
      <mat-card class="max-w-lg w-full text-center p-8">
        <mat-icon class="text-success text-6xl mb-4" style="font-size: 64px; width: 64px; height: 64px;">
          check_circle
        </mat-icon>
        <h1 class="text-2xl font-bold text-gray-900 mb-2">¡Contrato Creado!</h1>
        <p class="text-gray-600 mb-6">
          Hola <strong>{{ nombre() }}</strong>, tu solicitud de contrato ha sido procesada exitosamente.
        </p>
        <div class="bg-gray-50 rounded p-4 mb-6 text-left text-sm">
          <p><strong>ID del Tenant:</strong> {{ tenantId() }}</p>
          <p class="mt-1"><strong>Estado:</strong> Activo</p>
          <p class="mt-1"><strong>Próximos pasos:</strong> Recibirás un correo de confirmación con los detalles de acceso.</p>
        </div>
        <div class="flex gap-3 justify-center">
          <a mat-raised-button color="primary" routerLink="/contratacion" class="min-h-[44px]">
            Volver al portal
          </a>
          <a mat-stroked-button routerLink="/dashboard" class="min-h-[44px]">
            Ir al Dashboard
          </a>
        </div>
      </mat-card>
    </div>
  `,
})
export class ContractConfirmationComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);

  tenantId = signal('');
  nombre = signal('');

  ngOnInit(): void {
    const params = this.route.snapshot.queryParams;
    this.tenantId.set(params['tenantId'] || 'N/A');
    this.nombre.set(params['nombre'] || 'Cliente');
  }
}

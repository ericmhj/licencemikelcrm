import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-access-denied',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="flex flex-col items-center justify-center min-h-[60vh] text-center p-8">
      <h1 class="text-3xl font-bold text-red-600 mb-4">Acceso Denegado</h1>
      <p class="text-gray-600 mb-6">No tienes permisos para acceder a esta sección.</p>
      <a routerLink="/contratacion" class="text-primary underline hover:no-underline">
        Volver al portal público
      </a>
    </div>
  `,
})
export class AccessDeniedComponent {}

import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Clipboard, ClipboardModule } from '@angular/cdk/clipboard';

@Component({
  selector: 'app-contract-confirmation',
  standalone: true,
  imports: [
    CommonModule, RouterLink, MatCardModule, MatButtonModule,
    MatIconModule, MatDividerModule, MatSnackBarModule, ClipboardModule,
  ],
  template: `
    <div class="min-h-screen bg-[#F5F5F5] flex items-center justify-center p-4">
      <mat-card class="max-w-lg w-full p-8">
        <!-- Header -->
        <div class="text-center mb-6">
          <mat-icon class="text-green-600 mb-3" style="font-size: 64px; width: 64px; height: 64px;">
            check_circle
          </mat-icon>
          <h1 class="text-2xl font-bold text-gray-900">¡Tenant Creado Exitosamente!</h1>
          <p class="text-gray-600 mt-2">
            Hola <strong>{{ nombre() }}</strong>, tu organización ya está lista.
          </p>
        </div>

        <mat-divider class="my-4"></mat-divider>

        <!-- Datos de acceso -->
        <div class="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-4">
          <h2 class="text-sm font-semibold text-blue-800 mb-3 flex items-center gap-2">
            <mat-icon class="text-blue-600" style="font-size: 18px; width: 18px; height: 18px;">vpn_key</mat-icon>
            Datos de acceso a tu plataforma
          </h2>

          <div class="space-y-2 text-sm">
            <div class="flex justify-between items-center">
              <span class="text-gray-600">Dominio:</span>
              <div class="flex items-center gap-1">
                <a [href]="tenantUrl()" target="_blank" class="text-blue-600 font-medium hover:underline">
                  {{ tenantDomain() }}
                </a>
                <button mat-icon-button class="!w-7 !h-7" (click)="copiar(tenantUrl())" aria-label="Copiar URL">
                  <mat-icon style="font-size: 16px; width: 16px; height: 16px;">content_copy</mat-icon>
                </button>
              </div>
            </div>

            <div class="flex justify-between items-center">
              <span class="text-gray-600">Email:</span>
              <div class="flex items-center gap-1">
                <span class="font-medium">{{ email() }}</span>
                <button mat-icon-button class="!w-7 !h-7" (click)="copiar(email())" aria-label="Copiar email">
                  <mat-icon style="font-size: 16px; width: 16px; height: 16px;">content_copy</mat-icon>
                </button>
              </div>
            </div>

            <div class="flex justify-between items-center">
              <span class="text-gray-600">Contraseña temporal:</span>
              <div class="flex items-center gap-1">
                <code class="bg-white px-2 py-0.5 rounded border text-sm font-mono">admin123</code>
                <button mat-icon-button class="!w-7 !h-7" (click)="copiar('admin123')" aria-label="Copiar contraseña">
                  <mat-icon style="font-size: 16px; width: 16px; height: 16px;">content_copy</mat-icon>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Aviso de seguridad -->
        <div class="bg-amber-50 border border-amber-200 rounded-lg p-3 mb-4 text-xs text-amber-800 flex items-start gap-2">
          <mat-icon style="font-size: 16px; width: 16px; height: 16px; margin-top: 1px;">warning</mat-icon>
          <span>Cambia tu contraseña temporal inmediatamente después de iniciar sesión por primera vez.</span>
        </div>

        <!-- Info del tenant -->
        <div class="bg-gray-50 rounded-lg p-4 mb-6 text-sm space-y-1">
          <p><strong>ID del Tenant:</strong> {{ tenantId() }}</p>
          <p><strong>Slug:</strong> {{ slug() }}</p>
          <p><strong>Estado:</strong> <span class="text-green-600 font-medium">Activo</span></p>
        </div>

        <!-- Acciones -->
        <div class="flex flex-col gap-3">
          <a
            mat-raised-button
            color="primary"
            [href]="tenantUrl()"
            target="_blank"
            class="min-h-[44px] text-center"
          >
            <mat-icon class="mr-1">open_in_new</mat-icon>
            Abrir mi plataforma SGR
          </a>
          <a mat-stroked-button routerLink="/contratacion" class="min-h-[44px] text-center">
            Volver al portal
          </a>
        </div>
      </mat-card>
    </div>
  `,
})
export class ContractConfirmationComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly clipboard = inject(Clipboard);
  private readonly snackBar = inject(MatSnackBar);

  tenantId = signal('');
  nombre = signal('');
  email = signal('');
  slug = signal('');

  tenantDomain = computed(() => {
    const s = this.slug();
    return s ? `${s}.localhost:3000` : 'localhost:3000';
  });

  tenantUrl = computed(() => {
    return `http://${this.tenantDomain()}/login`;
  });

  ngOnInit(): void {
    const params = this.route.snapshot.queryParams;
    this.tenantId.set(params['tenantId'] || 'N/A');
    this.nombre.set(params['nombre'] || 'Cliente');
    this.email.set(params['email'] || '');
    this.slug.set(params['slug'] || '');
  }

  copiar(texto: string): void {
    this.clipboard.copy(texto);
    this.snackBar.open('Copiado al portapapeles', '', { duration: 2000 });
  }
}

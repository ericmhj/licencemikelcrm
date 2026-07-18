import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';
import { MODULOS_DISPONIBLES, PAQUETES_CREDITOS, ModuloDisponible, PaqueteCreditos } from '../../../../core/models/calculator.model';
import { CurrencyEurPipe } from '../../../../shared/pipes/currency-eur.pipe';
import { PlanService, Plan } from '../../../../core/services/plan.service';

@Component({
  selector: 'app-contract-form',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatIconModule, MatSelectModule,
    MatSnackBarModule, MatProgressSpinnerModule, CurrencyEurPipe,
  ],
  template: `
    <div class="min-h-screen bg-[#F5F5F5]">
      <header class="bg-white shadow-sm border-b">
        <div class="max-w-3xl mx-auto px-4 py-4 flex items-center gap-3">
          <button mat-icon-button (click)="volver()">
            <mat-icon>arrow_back</mat-icon>
          </button>
          <h1 class="text-xl font-semibold text-primary">Solicitar Contrato</h1>
        </div>
      </header>

      <main class="max-w-3xl mx-auto px-4 py-8">
        <!-- Resumen de selección -->
        <mat-card class="mb-6">
          <mat-card-header>
            <mat-card-title>Resumen de tu selección</mat-card-title>
          </mat-card-header>
          <mat-card-content class="p-4">
            <ul class="space-y-1">
              @for (mod of selectedModules(); track mod.id) {
                <li class="flex justify-between text-sm">
                  <span>{{ mod.nombre }}</span>
                  <span class="font-medium">{{ mod.precioMensual | currencyEur }}/mes</span>
                </li>
              }
            </ul>
            @if (selectedPackage()) {
              <div class="mt-3 pt-3 border-t text-sm flex justify-between">
                <span>{{ selectedPackage()!.nombre }} ({{ selectedPackage()!.creditos }} + {{ selectedPackage()!.bonus }} bonus)</span>
                <span class="font-medium">{{ selectedPackage()!.precioAnual | currencyEur }}/año</span>
              </div>
            }
            <div class="mt-3 pt-3 border-t flex justify-between font-bold">
              <span>Cuota mensual estimada</span>
              <span class="text-primary">{{ cuotaMensual() | currencyEur }}</span>
            </div>
          </mat-card-content>
        </mat-card>

        <!-- Formulario de datos -->
        <mat-card>
          <mat-card-header>
            <mat-card-title>Datos de tu empresa</mat-card-title>
          </mat-card-header>
          <mat-card-content class="p-4 space-y-4">
            <mat-form-field class="w-full" appearance="outline">
              <mat-label>Nombre de la empresa</mat-label>
              <input matInput [(ngModel)]="formData.nombre" required placeholder="Ej: Mi Empresa S.A." />
            </mat-form-field>

            <mat-form-field class="w-full" appearance="outline">
              <mat-label>Email de contacto</mat-label>
              <input matInput [(ngModel)]="formData.email" required type="email" placeholder="admin@empresa.com" />
            </mat-form-field>

            <mat-form-field class="w-full" appearance="outline">
              <mat-label>Plan</mat-label>
              <mat-select [(ngModel)]="formData.plan" required>
                @for (plan of planes(); track plan.codigo) {
                  <mat-option [value]="plan.codigo">{{ plan.nombre }}</mat-option>
                }
              </mat-select>
            </mat-form-field>

            <div class="pt-4">
              <button
                mat-raised-button
                color="primary"
                class="w-full min-h-[44px]"
                [disabled]="!isFormValid() || submitting()"
                (click)="enviarSolicitud()"
              >
                @if (submitting()) {
                  <mat-spinner diameter="20" class="inline-block mr-2"></mat-spinner>
                  Procesando...
                } @else {
                  Enviar solicitud de contrato
                }
              </button>
            </div>

            @if (errorMessage()) {
              <div class="mt-4 p-3 bg-red-50 border border-red-200 rounded text-red-700 text-sm">
                {{ errorMessage() }}
              </div>
            }
          </mat-card-content>
        </mat-card>
      </main>
    </div>
  `,
})
export class ContractFormComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly http = inject(HttpClient);
  private readonly snackBar = inject(MatSnackBar);
  private readonly planService = inject(PlanService);

  selectedModules = signal<ModuloDisponible[]>([]);
  selectedPackage = signal<PaqueteCreditos | null>(null);
  submitting = signal(false);
  errorMessage = signal<string | null>(null);
  planes = signal<Plan[]>([]);

  formData = {
    nombre: '',
    email: '',
    plan: '',
    modalidad: 'ESTANDAR',
  };

  cuotaMensual = signal(0);

  ngOnInit(): void {
    // Load plans catalog
    this.planService.getAll().subscribe({
      next: (plans) => this.planes.set(plans),
      error: () => console.error('Error cargando catálogo de planes'),
    });

    const params = this.route.snapshot.queryParams;
    const moduleIds = (params['modules'] || '').split(',').filter(Boolean);
    const packageId = params['package'] || null;

    const modules = MODULOS_DISPONIBLES.filter(m => moduleIds.includes(m.id));
    this.selectedModules.set(modules.length > 0 ? modules : MODULOS_DISPONIBLES.filter(m => m.obligatorio));

    if (packageId) {
      const pkg = PAQUETES_CREDITOS.find(p => p.id === packageId) || null;
      this.selectedPackage.set(pkg);
    }

    // Calculate cuota
    const totalModulos = this.selectedModules().reduce((sum, m) => sum + m.precioMensual * 12, 0);
    const totalPaquete = this.selectedPackage()?.precioAnual ?? 0;
    this.cuotaMensual.set((totalModulos + totalPaquete) / 12);
  }

  isFormValid(): boolean {
    return this.formData.nombre.trim().length > 0 &&
           this.formData.email.trim().length > 0 &&
           this.formData.email.includes('@');
  }

  volver(): void {
    this.router.navigate(['/contratacion']);
  }

  async enviarSolicitud(): Promise<void> {
    if (!this.isFormValid()) return;
    this.submitting.set(true);
    this.errorMessage.set(null);

    const baseUrl = environment.apiUrl;

    try {
      // 1. Crear tenant
      const tenantResponse: any = await this.http.post(`${baseUrl}/api/v1/tenants`, {
        nombre: this.formData.nombre,
        emailContacto: this.formData.email,
        modalidadApertura: this.formData.modalidad,
      }).toPromise();

      const tenantId = tenantResponse.id;

      // 2. Procesar apertura
      await this.http.post(`${baseUrl}/api/v1/tenants/${tenantId}/apertura`, {
        modalidad: this.formData.modalidad,
        metodoPago: 'PENDIENTE',
      }).toPromise();

      // 3. Crear contratos por módulo seleccionado
      for (const mod of this.selectedModules()) {
        await this.http.post(`${baseUrl}/api/v1/tenants/${tenantId}/contracts`, {
          tipo: 'MODULO',
          modulo: mod.nombre,
          cuotaMensual: mod.precioMensual,
          fechaInicio: new Date().toISOString().split('T')[0],
          renovacionAuto: true,
        }).toPromise();
      }

      // 4. Si hay paquete de créditos, adquirirlo
      const pkg = this.selectedPackage();
      if (pkg) {
        await this.http.post(`${baseUrl}/api/v1/tenants/${tenantId}/credits/packages`, {
          cantidadCreditos: pkg.creditos,
          metodoPago: 'PENDIENTE',
        }).toPromise();
      }

      this.submitting.set(false);
      this.snackBar.open('¡Solicitud enviada! Tu contrato ha sido creado.', 'OK', { duration: 5000 });

      // Redirigir a confirmación con datos para acceso al tenant
      const slug = tenantResponse.slug || this.formData.nombre.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
      this.router.navigate(['/contratacion/confirmacion'], {
        queryParams: {
          tenantId,
          nombre: this.formData.nombre,
          email: this.formData.email,
          slug,
        },
      });

    } catch (err: any) {
      this.submitting.set(false);
      const message = err?.error?.message || 'Error al procesar la solicitud. Intenta de nuevo.';
      this.errorMessage.set(message);
    }
  }
}

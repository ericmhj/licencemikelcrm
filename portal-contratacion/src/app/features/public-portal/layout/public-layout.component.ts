import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ModuleCardsComponent } from '../components/module-cards/module-cards.component';
import { CreditPackagesComponent } from '../components/credit-packages/credit-packages.component';
import { CalculatorComponent } from '../components/calculator/calculator.component';
import { DiscountPolicyComponent } from '../components/discount-policy/discount-policy.component';
import { ServiceSummaryComponent } from '../components/service-summary/service-summary.component';
import { PaymentDialogComponent, PaymentDialogData, PaymentDialogResult } from '../components/payment-dialog/payment-dialog.component';
import { CalculatorStateService } from '../services/calculator-state.service';
import { MODULOS_DISPONIBLES, ModuloDisponible, PaqueteCreditos } from '../../../core/models/calculator.model';
import { CurrencyEurPipe } from '../../../shared/pipes/currency-eur.pipe';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-public-layout',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink,
    MatToolbarModule, MatButtonModule, MatCardModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatIconModule, MatSnackBarModule, MatProgressSpinnerModule,
    MatDialogModule,
    ModuleCardsComponent, CreditPackagesComponent,
    CalculatorComponent, DiscountPolicyComponent,
    ServiceSummaryComponent, CurrencyEurPipe,
  ],
  providers: [CalculatorStateService],
  template: `
    <div class="min-h-screen bg-[#F5F5F5]">
      <!-- Header -->
      <header class="bg-white shadow-sm border-b sticky top-0 z-10">
        <div class="max-w-7xl mx-auto px-4 py-4 flex items-center justify-between">
          <div class="flex items-center gap-3">
            <h1 class="text-xl font-semibold text-primary">Portal de Contratación</h1>
            <span class="text-gray-400 hidden sm:inline">—</span>
            <span class="text-gray-700 font-medium hidden sm:inline">Mikel CRM</span>
          </div>
          <a routerLink="/login" class="text-primary hover:underline font-medium text-sm">Iniciar sesión</a>
        </div>
      </header>

      <main class="max-w-7xl mx-auto px-4 py-8">
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">

          <!-- Columna izquierda: Módulos + Créditos + Descuentos -->
          <section class="lg:col-span-2 space-y-6">
            <app-module-cards
              [modules]="modules"
              (selectionChanged)="onModulesChanged($event)"
            />
            <app-credit-packages
              (packageChanged)="onPackageChanged($event)"
            />
            <app-discount-policy />
          </section>

          <!-- Columna derecha: Calculadora + Formulario -->
          <aside class="lg:col-span-1 space-y-6">
            <!-- Calculadora (siempre visible) -->
            <div class="sticky top-20">
              <app-calculator
                [subtotales]="state.subtotales()"
                [totalAnual]="state.totalAnual()"
                [cuotaMensual]="state.cuotaMensual()"
              />

              <!-- Resumen -->
              <div class="mt-4">
                <app-service-summary
                  [subtotales]="state.subtotales()"
                  [totalAnual]="state.totalAnual()"
                  [cuotaMensual]="state.cuotaMensual()"
                  [selectedPackage]="state.selectedPackage()"
                />
              </div>

              <!-- Botón Solicitar / Formulario inline -->
              <div class="mt-4">
                @if (!showForm()) {
                  <button
                    mat-raised-button
                    color="primary"
                    class="w-full min-h-[48px] text-base"
                    (click)="showForm.set(true)"
                  >
                    Solicitar contrato
                    <mat-icon class="ml-1">arrow_downward</mat-icon>
                  </button>
                } @else {
                  <!-- Formulario de solicitud inline -->
                  <mat-card class="animate-fadeIn">
                    <mat-card-header>
                      <mat-card-title class="text-base">Datos de tu empresa</mat-card-title>
                    </mat-card-header>
                    <mat-card-content class="p-4 space-y-3">
                      <mat-form-field class="w-full" appearance="outline">
                        <mat-label>Nombre de la empresa</mat-label>
                        <input matInput [(ngModel)]="formData.nombre" required
                               placeholder="Ej: Mi Empresa S.A." />
                      </mat-form-field>

                      <mat-form-field class="w-full" appearance="outline">
                        <mat-label>Email de contacto</mat-label>
                        <input matInput [(ngModel)]="formData.email" required
                               type="email" placeholder="admin&#64;empresa.com" />
                      </mat-form-field>

                      <mat-form-field class="w-full" appearance="outline">
                        <mat-label>Modalidad de apertura</mat-label>
                        <mat-select [(ngModel)]="formData.modalidad" required>
                          <mat-option value="ESTANDAR">Estándar — 400 € (2 créditos)</mat-option>
                          <mat-option value="PERSONALIZADO">Personalizada — 1.400 € (10 créditos)</mat-option>
                        </mat-select>
                      </mat-form-field>

                      <div class="flex gap-2 pt-2">
                        <button
                          mat-raised-button
                          color="primary"
                          class="flex-1 min-h-[44px]"
                          [disabled]="!isFormValid() || submitting()"
                          (click)="enviarSolicitud()"
                        >
                          @if (submitting()) {
                            Procesando...
                          } @else {
                            Enviar solicitud
                          }
                        </button>
                        <button
                          mat-stroked-button
                          class="min-h-[44px]"
                          (click)="showForm.set(false)"
                          [disabled]="submitting()"
                        >
                          Cancelar
                        </button>
                      </div>

                      @if (errorMessage()) {
                        <div class="p-3 bg-red-50 border border-red-200 rounded text-red-700 text-sm">
                          {{ errorMessage() }}
                        </div>
                      }

                      @if (successMessage()) {
                        <div class="p-3 bg-green-50 border border-green-200 rounded text-green-700 text-sm">
                          {{ successMessage() }}
                        </div>
                      }
                    </mat-card-content>
                  </mat-card>
                }
              </div>
            </div>
          </aside>
        </div>
      </main>

      <!-- Pantalla de bienvenida (reemplaza todo tras éxito) -->
      @if (showWelcome()) {
        <div class="fixed inset-0 z-50 bg-white flex flex-col animate-fadeIn">
          <header class="bg-primary text-white p-4 text-center">
            <h1 class="text-2xl font-bold">🎉 ¡Bienvenido a Mikel CRM!</h1>
            <p class="text-sm opacity-90 mt-1">Tu servicio ha sido activado exitosamente</p>
          </header>
          <div class="flex-1 overflow-y-auto p-6 max-w-2xl mx-auto w-full">
            <pre class="whitespace-pre-wrap font-mono text-sm bg-gray-50 border rounded-lg p-6 leading-relaxed">{{ welcomeDetails() }}</pre>
          </div>
          <footer class="p-4 border-t bg-gray-50 flex justify-center gap-4">
            <button mat-raised-button color="primary" (click)="showWelcome.set(false)" class="min-h-[44px]">
              Volver al portal
            </button>
          </footer>
        </div>
      }
    </div>
  `,
  styles: [`
    .animate-fadeIn {
      animation: fadeIn 0.3s ease-in-out;
    }
    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(-10px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `]
})
export class PublicLayoutComponent {
  readonly state = inject(CalculatorStateService);
  readonly modules = MODULOS_DISPONIBLES;
  private readonly http = inject(HttpClient);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);

  showForm = signal(false);
  submitting = signal(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  showWelcome = signal(false);
  welcomeDetails = signal<string>('');

  formData = {
    nombre: '',
    email: '',
    modalidad: 'ESTANDAR',
  };

  onModulesChanged(selected: ModuloDisponible[]): void {
    const currentSelected = this.state.selectedModules();
    for (const mod of this.modules) {
      if (mod.obligatorio) continue;
      const isInState = currentSelected.some(m => m.id === mod.id);
      const isInSelection = selected.some(m => m.id === mod.id);
      if (isInState !== isInSelection) {
        this.state.toggleModule(mod);
      }
    }
  }

  onPackageChanged(pkg: PaqueteCreditos | null): void {
    this.state.selectPackage(pkg);
  }

  isFormValid(): boolean {
    return this.formData.nombre.trim().length > 0 &&
           this.formData.email.trim().length > 0 &&
           this.formData.email.includes('@');
  }

  async enviarSolicitud(): Promise<void> {
    if (!this.isFormValid()) return;
    this.errorMessage.set(null);
    this.successMessage.set(null);

    // Calculate the first month payment amount
    const amount = this.state.cuotaMensual();

    // Open payment dialog
    const dialogRef = this.dialog.open(PaymentDialogComponent, {
      width: '450px',
      disableClose: true,
      data: {
        amount: amount,
        description: `Primera cuota mensual - ${this.formData.nombre}`,
      } as PaymentDialogData,
    });

    const result: PaymentDialogResult | undefined = await dialogRef.afterClosed().toPromise();

    if (!result || !result.approved) {
      // User cancelled or payment was rejected
      return;
    }

    // Payment approved → proceed with tenant creation
    this.submitting.set(true);

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
        metodoPago: 'TARJETA',
        transactionId: result.transactionId,
      }).toPromise();

      // 3. Crear contratos por módulo seleccionado
      for (const mod of this.state.selectedModules()) {
        await this.http.post(`${baseUrl}/api/v1/tenants/${tenantId}/contracts`, {
          tipo: 'MODULO',
          modulo: mod.nombre,
          cuotaMensual: mod.precioMensual,
          fechaInicio: new Date().toISOString().split('T')[0],
          renovacionAuto: true,
        }).toPromise();
      }

      // 4. Si hay paquete de créditos
      const pkg = this.state.selectedPackage();
      if (pkg) {
        await this.http.post(`${baseUrl}/api/v1/tenants/${tenantId}/credits/packages`, {
          cantidadCreditos: pkg.creditos,
          metodoPago: 'TARJETA',
          transactionId: result.transactionId,
        }).toPromise();
      }

      // 5. Send welcome email
      await this.http.post(`${baseUrl}/api/v1/notifications/welcome`, {
        email: this.formData.email,
        nombre: this.formData.nombre,
        tenantId: tenantId,
        modalidad: this.formData.modalidad,
        creditosBienvenida: this.formData.modalidad === 'ESTANDAR' ? 2 : 10,
        modulos: this.state.selectedModules().map(m => m.nombre),
        cuotaMensual: this.state.cuotaMensual(),
      }).toPromise();

      this.submitting.set(false);

      // Generate welcome details
      const fecha = new Date().toLocaleDateString('es-ES');
      const lines = [
        '═══════════════════════════════════════════════',
        '   🎉 ¡FELICIDADES POR TU NUEVO SERVICIO!',
        '═══════════════════════════════════════════════',
        '',
        `Fecha de contratación: ${fecha}`,
        `ID del Tenant: ${tenantId}`,
        '',
        '───────────────────────────────────────────────',
        '   DATOS DE LA EMPRESA',
        '───────────────────────────────────────────────',
        `Empresa: ${this.formData.nombre}`,
        `Email: ${this.formData.email}`,
        `Modalidad: ${this.formData.modalidad === 'ESTANDAR' ? 'Estándar (400 €)' : 'Personalizada (1.400 €)'}`,
        `Créditos de bienvenida: ${this.formData.modalidad === 'ESTANDAR' ? '2' : '10'}`,
        '',
        '───────────────────────────────────────────────',
        '   MÓDULOS CONTRATADOS',
        '───────────────────────────────────────────────',
      ];

      for (const mod of this.state.selectedModules()) {
        lines.push(`  ✓ ${mod.nombre} — ${mod.precioMensual.toFixed(2)} €/mes`);
      }

      const pkgInfo = this.state.selectedPackage();
      if (pkgInfo) {
        lines.push('');
        lines.push('───────────────────────────────────────────────');
        lines.push('   PAQUETE DE CRÉDITOS');
        lines.push('───────────────────────────────────────────────');
        lines.push(`  ✓ ${pkgInfo.nombre}: ${pkgInfo.creditos} créditos + ${pkgInfo.bonus} bonus`);
        lines.push(`    Precio: ${pkgInfo.precioAnual.toFixed(2)} €/año`);
      }

      lines.push('');
      lines.push('───────────────────────────────────────────────');
      lines.push('   RESUMEN ECONÓMICO');
      lines.push('───────────────────────────────────────────────');
      lines.push(`  Total anual: ${this.state.totalAnual().toFixed(2)} €`);
      lines.push(`  Cuota mensual: ${this.state.cuotaMensual().toFixed(2)} €`);
      lines.push('');
      lines.push('───────────────────────────────────────────────');
      lines.push('   POLÍTICA DE DESCUENTOS');
      lines.push('───────────────────────────────────────────────');
      lines.push('  • 10% descuento: pago antes de la fecha límite');
      lines.push('  • 3% descuento: pago en la fecha límite');
      lines.push('  • 0% descuento: pago después de la fecha límite');
      lines.push('');
      lines.push('───────────────────────────────────────────────');
      lines.push('   PRÓXIMOS PASOS');
      lines.push('───────────────────────────────────────────────');
      lines.push('  1. Recibirás un email de confirmación en breve');
      lines.push('  2. Tu CRM estará activo de inmediato');
      lines.push('  3. Primer cobro de cuota: en 30 días');
      lines.push('');
      lines.push('═══════════════════════════════════════════════');
      lines.push('  Gracias por confiar en Mikel CRM');
      lines.push('═══════════════════════════════════════════════');

      this.welcomeDetails.set(lines.join('\n'));
      this.showWelcome.set(true);
      this.showForm.set(false);
      this.snackBar.open('¡Solicitud procesada exitosamente!', 'OK', { duration: 5000 });

    } catch (err: any) {
      this.submitting.set(false);
      const message = err?.error?.message || 'Error al procesar la solicitud. Intenta de nuevo.';
      this.errorMessage.set(message);
    }
  }
}

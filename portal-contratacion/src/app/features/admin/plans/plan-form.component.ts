import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { PlanService, CreatePlanRequest } from '../../../core/services/plan.service';

const ALL_ROLES = ['tecnico', 'asistente', 'manager', 'admin', 'superusuario'];

@Component({
  selector: 'app-plan-form',
  standalone: true,
  imports: [CommonModule, FormsModule, MatCardModule, MatButtonModule, MatIconModule, MatSnackBarModule, MatCheckboxModule],
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title>{{ isEdit ? 'Editar Plan' : 'Nuevo Plan' }}</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        <form (ngSubmit)="save()" class="plan-form">
          <div class="field">
            <label>Código</label>
            <input [(ngModel)]="plan.codigo" name="codigo" [disabled]="isEdit" required placeholder="PLAN_CUSTOM" />
          </div>
          <div class="field">
            <label>Nombre</label>
            <input [(ngModel)]="plan.nombre" name="nombre" required placeholder="Plan Personalizado" />
          </div>
          <div class="field">
            <label>Descripción</label>
            <textarea [(ngModel)]="plan.descripcion" name="descripcion" rows="2"></textarea>
          </div>
          <div class="field-row">
            <div class="field">
              <label>Créditos mensuales (-1 = ilimitado)</label>
              <input type="number" [(ngModel)]="plan.creditosMensuales" name="creditos" required />
            </div>
            <div class="field">
              <label>Descargas gratis (-1 = ilimitado)</label>
              <input type="number" [(ngModel)]="plan.maxFreeDownloads" name="downloads" required />
            </div>
            <div class="field">
              <label>Máx. usuarios (-1 = ilimitado)</label>
              <input type="number" [(ngModel)]="plan.maxUsuarios" name="usuarios" required />
            </div>
          </div>
          <div class="field">
            <label>Precio mensual (MXN)</label>
            <input type="number" [(ngModel)]="plan.precioMensual" name="precio" required step="0.01" />
          </div>

          <div class="field">
            <label>Roles autorizados</label>
            <div class="roles-grid">
              @for (role of allRoles; track role) {
                <mat-checkbox
                  [checked]="plan.rolesAutorizados.includes(role)"
                  (change)="toggleRole(role, $event.checked)">
                  {{ role }}
                </mat-checkbox>
              }
            </div>
          </div>

          <div class="actions">
            <button mat-raised-button color="primary" type="submit" [disabled]="!plan.codigo || !plan.nombre">
              {{ isEdit ? 'Guardar Cambios' : 'Crear Plan' }}
            </button>
            <button mat-button type="button" (click)="cancel()">Cancelar</button>
          </div>
        </form>
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .plan-form { display: flex; flex-direction: column; gap: 16px; max-width: 600px; }
    .field { display: flex; flex-direction: column; gap: 4px; }
    .field input, .field textarea { padding: 8px; border: 1px solid #ccc; border-radius: 4px; }
    .field-row { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px; }
    .roles-grid { display: flex; flex-wrap: wrap; gap: 8px; }
    .actions { display: flex; gap: 8px; margin-top: 16px; }
    label { font-weight: 500; font-size: 14px; color: #333; }
  `]
})
export class PlanFormComponent implements OnInit {
  private planService = inject(PlanService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);

  isEdit = false;
  allRoles = ALL_ROLES;

  plan: CreatePlanRequest = {
    codigo: '',
    nombre: '',
    descripcion: '',
    creditosMensuales: 100,
    maxFreeDownloads: 3,
    maxUsuarios: 5,
    rolesAutorizados: ['tecnico'],
    precioMensual: 499,
  };

  ngOnInit() {
    const codigo = this.route.snapshot.params['codigo'];
    if (codigo) {
      this.isEdit = true;
      this.planService.getByCode(codigo).subscribe({
        next: (p) => {
          this.plan = {
            codigo: p.codigo,
            nombre: p.nombre,
            descripcion: p.descripcion,
            creditosMensuales: p.creditosMensuales,
            maxFreeDownloads: p.maxFreeDownloads,
            maxUsuarios: p.maxUsuarios,
            rolesAutorizados: [...p.rolesAutorizados],
            precioMensual: p.precioMensual,
          };
        },
        error: () => this.snackBar.open('Plan no encontrado', 'Cerrar', { duration: 3000 })
      });
    }
  }

  toggleRole(role: string, checked: boolean | null) {
    if (checked) {
      if (!this.plan.rolesAutorizados.includes(role)) {
        this.plan.rolesAutorizados.push(role);
      }
    } else {
      this.plan.rolesAutorizados = this.plan.rolesAutorizados.filter(r => r !== role);
    }
  }

  save() {
    const obs = this.isEdit
      ? this.planService.update(this.plan.codigo, this.plan)
      : this.planService.create(this.plan);

    obs.subscribe({
      next: () => {
        this.snackBar.open(this.isEdit ? 'Plan actualizado' : 'Plan creado', 'OK', { duration: 3000 });
        this.router.navigate(['/admin/planes']);
      },
      error: (err) => {
        const msg = err.status === 409 ? 'Ya existe un plan con ese código' : 'Error al guardar';
        this.snackBar.open(msg, 'Cerrar', { duration: 3000 });
      }
    });
  }

  cancel() {
    this.router.navigate(['/admin/planes']);
  }
}

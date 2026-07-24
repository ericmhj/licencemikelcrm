import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { FuncionRolesService } from '../../../core/services/funcion-roles.service';
import {
  CreateFuncionRolesRequest,
  UpdateFuncionRolesRequest,
  VALID_ROLES,
} from '../../../core/models/funcion-roles.model';

@Component({
  selector: 'app-funcion-roles-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    MatSnackBarModule,
  ],
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title>{{ isEditMode ? 'Editar Funcion-Roles' : 'Nueva Funcion-Roles' }}</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        <form [formGroup]="form" (ngSubmit)="onSubmit()" class="funcion-roles-form">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Código</mat-label>
            <input matInput formControlName="codigo" placeholder="FUNC_BASICA" />
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Descripción</mat-label>
            <textarea matInput formControlName="descripcion" rows="3" placeholder="Descripción de la relación"></textarea>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Funcionalidad</mat-label>
            <input matInput formControlName="funcionalidad" placeholder="Nombre de la funcionalidad" />
          </mat-form-field>

          <div class="roles-section">
            <label class="roles-label">Roles</label>
            <div class="roles-grid">
              @for (role of validRoles; track role) {
                <mat-checkbox
                  [checked]="selectedRoles.includes(role)"
                  (change)="toggleRole(role, $event.checked)">
                  {{ role }}
                </mat-checkbox>
              }
            </div>
            @if (showRolesError) {
              <p class="roles-error">Debe seleccionar al menos un rol</p>
            }
          </div>

          <div class="actions">
            <button mat-raised-button color="primary" type="submit"
              [disabled]="form.invalid || selectedRoles.length === 0">
              {{ isEditMode ? 'Guardar Cambios' : 'Crear' }}
            </button>
            <button mat-button type="button" (click)="cancel()">Cancelar</button>
          </div>
        </form>
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .funcion-roles-form {
      display: flex;
      flex-direction: column;
      gap: 16px;
      max-width: 600px;
      padding-top: 16px;
    }
    .full-width { width: 100%; }
    .roles-section {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    .roles-label {
      font-weight: 500;
      font-size: 14px;
      color: #333;
    }
    .roles-grid {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }
    .roles-error {
      color: #f44336;
      font-size: 12px;
      margin: 4px 0 0 0;
    }
    .actions {
      display: flex;
      gap: 8px;
      margin-top: 16px;
    }
  `],
})
export class FuncionRolesFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly funcionRolesService = inject(FuncionRolesService);

  readonly validRoles = VALID_ROLES;

  /** Whether the URL indicates edit mode (presence of :codigo param) */
  isEditMode = false;

  /** The codigo from the URL param for edit mode */
  private editCodigo = '';

  /** Tracks whether form submission was attempted (for roles validation display) */
  submitted = false;

  selectedRoles: string[] = [];

  form: FormGroup = this.fb.group({
    codigo: ['', Validators.required],
    descripcion: [''],
    funcionalidad: ['', Validators.required],
  });

  get showRolesError(): boolean {
    return this.submitted && this.selectedRoles.length === 0;
  }

  ngOnInit(): void {
    const codigo = this.route.snapshot.params['codigo'];

    if (codigo) {
      // Edit mode: URL has :codigo param
      this.isEditMode = true;
      this.editCodigo = codigo;

      // Always disable codigo field based on URL context (Requirement 9.3)
      this.form.get('codigo')!.disable();

      // Load existing entity data
      this.funcionRolesService.getByCode(codigo).subscribe({
        next: (entity) => {
          this.form.patchValue({
            codigo: entity.codigo,
            descripcion: entity.descripcion,
            funcionalidad: entity.funcionalidad,
          });
          this.selectedRoles = [...entity.roles];
        },
        error: () => {
          this.snackBar.open('Recurso no encontrado', 'Cerrar', { duration: 3000 });
          this.router.navigate(['/admin/funcion_roles']);
        },
      });
    }
  }

  toggleRole(role: string, checked: boolean | null): void {
    if (checked) {
      if (!this.selectedRoles.includes(role)) {
        this.selectedRoles = [...this.selectedRoles, role];
      }
    } else {
      this.selectedRoles = this.selectedRoles.filter((r) => r !== role);
    }
  }

  onSubmit(): void {
    this.submitted = true;

    if (this.selectedRoles.length === 0) {
      return;
    }

    if (this.form.invalid) {
      return;
    }

    if (this.isEditMode) {
      this.updateEntity();
    } else {
      this.createEntity();
    }
  }

  private createEntity(): void {
    const formValue = this.form.getRawValue();
    const request: CreateFuncionRolesRequest = {
      codigo: formValue.codigo,
      descripcion: formValue.descripcion || '',
      funcionalidad: formValue.funcionalidad,
      roles: this.selectedRoles,
    };

    this.funcionRolesService.create(request).subscribe({
      next: () => {
        this.snackBar.open('Funcion-Roles creada correctamente', 'OK', { duration: 3000 });
        this.router.navigate(['/admin/funcion_roles']);
      },
      error: (err) => this.handleSubmitError(err),
    });
  }

  private updateEntity(): void {
    const formValue = this.form.getRawValue();
    const request: UpdateFuncionRolesRequest = {
      descripcion: formValue.descripcion || '',
      funcionalidad: formValue.funcionalidad,
      roles: this.selectedRoles,
    };

    this.funcionRolesService.update(this.editCodigo, request).subscribe({
      next: () => {
        this.snackBar.open('Funcion-Roles actualizada correctamente', 'OK', { duration: 3000 });
        this.router.navigate(['/admin/funcion_roles']);
      },
      error: (err) => this.handleSubmitError(err),
    });
  }

  private handleSubmitError(err: { status: number; message: string; error?: any }): void {
    if (err.status === 0) {
      // Network error - preserve form state (Requirement 9.8)
      this.snackBar.open('Error de conectividad', 'Cerrar', { duration: 5000 });
      return;
    }

    if (err.status === 409) {
      // Distinguish between duplicate codigo and duplicate role set (Requirement 9.6)
      const errorBody = err.error;
      if (errorBody?.error === 'DUPLICATE_CODIGO') {
        this.snackBar.open('Ya existe una relación con ese código', 'Cerrar', { duration: 5000 });
      } else if (errorBody?.error === 'DUPLICATE_ROLE_SET') {
        const conflicting = errorBody?.conflictingCodigo;
        const msg = conflicting
          ? `Ya existe una relación con ese conjunto de roles: ${conflicting}`
          : 'Ya existe una relación con ese conjunto de roles';
        this.snackBar.open(msg, 'Cerrar', { duration: 5000 });
      } else {
        this.snackBar.open('Conflicto: el recurso ya existe o hay un duplicado', 'Cerrar', { duration: 5000 });
      }
      return;
    }

    // Generic error
    this.snackBar.open(err.message || 'Error al guardar', 'Cerrar', { duration: 3000 });
  }

  cancel(): void {
    this.router.navigate(['/admin/funcion_roles']);
  }
}

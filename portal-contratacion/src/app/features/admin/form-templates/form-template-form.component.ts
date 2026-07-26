import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import {
  FormTemplateService,
  FormTemplate,
  CreateFormTemplateRequest,
  UpdateFormTemplateRequest,
} from '../../../core/services/form-template.service';

@Component({
  selector: 'app-form-template-form',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title>{{ isEditMode ? 'Editar' : 'Nuevo' }} Formulario Padre</mat-card-title>
        <mat-card-subtitle>
          {{ isEditMode ? 'Actualizar template existente (incrementa versión)' : 'Crear un nuevo template de formulario' }}
        </mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        @if (loadingExisting) {
          <div class="loading-container">
            <mat-spinner diameter="40"></mat-spinner>
            <p>Cargando template...</p>
          </div>
        } @else {
          <form (ngSubmit)="onSubmit()" class="template-form">
            @if (!isEditMode) {
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Tipo de formulario (form_type)</mat-label>
                <input matInput [(ngModel)]="formData.form_type" name="form_type" required
                       placeholder="ej: nom025, nom035, custom" />
                <mat-hint>Identificador único del tipo. No se puede cambiar después.</mat-hint>
              </mat-form-field>
            }

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Nombre</mat-label>
              <input matInput [(ngModel)]="formData.name" name="name" required
                     placeholder="ej: NOM-025 Iluminación" />
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Descripción (opcional)</mat-label>
              <textarea matInput [(ngModel)]="formData.description" name="description"
                        rows="3" placeholder="Descripción del template"></textarea>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>HTML del formulario</mat-label>
              <textarea matInput [(ngModel)]="formData.html_content" name="html_content" required
                        rows="15" class="code-textarea"
                        placeholder="Pegue aquí el HTML completo del formulario..."></textarea>
              <mat-hint>
                Incluya secciones con class="section-heading" o data-section="nombre" y campos con atributo name.
              </mat-hint>
            </mat-form-field>

            @if (error) {
              <div class="error-box">
                <mat-icon>error</mat-icon>
                <span>{{ error }}</span>
              </div>
            }

            @if (existingTemplate && isEditMode) {
              <div class="info-box">
                <mat-icon>info</mat-icon>
                <span>Versión actual: v{{ existingTemplate.currentVersion }}. Al guardar se creará la v{{ existingTemplate.currentVersion + 1 }}.</span>
              </div>
            }

            <div class="form-actions">
              <button mat-raised-button color="primary" type="submit" [disabled]="submitting">
                @if (submitting) {
                  <mat-spinner diameter="20" class="inline-spinner"></mat-spinner>
                }
                {{ isEditMode ? 'Actualizar Template' : 'Crear Template' }}
              </button>
              <a mat-button routerLink="/admin/form_templates">Cancelar</a>
            </div>
          </form>
        }
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .template-form {
      display: flex;
      flex-direction: column;
      gap: 8px;
      max-width: 800px;
    }
    .full-width { width: 100%; }
    .code-textarea {
      font-family: 'Fira Code', 'Cascadia Code', monospace;
      font-size: 13px;
      line-height: 1.5;
    }
    .form-actions {
      display: flex;
      gap: 12px;
      align-items: center;
      margin-top: 16px;
    }
    .error-box {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px 16px;
      background: #fef2f2;
      border: 1px solid #fecaca;
      border-radius: 8px;
      color: #dc2626;
      font-size: 14px;
    }
    .info-box {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px 16px;
      background: #eff6ff;
      border: 1px solid #bfdbfe;
      border-radius: 8px;
      color: #1d4ed8;
      font-size: 14px;
    }
    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
      padding: 48px;
      color: #666;
    }
    .inline-spinner { display: inline-block; margin-right: 8px; }
  `],
})
export class FormTemplateFormComponent implements OnInit {
  private service = inject(FormTemplateService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);

  isEditMode = false;
  loadingExisting = false;
  submitting = false;
  error = '';
  existingTemplate: FormTemplate | null = null;

  formData: CreateFormTemplateRequest = {
    form_type: '',
    name: '',
    description: '',
    html_content: '',
  };

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.loadExisting(id);
    }
  }

  loadExisting(id: string) {
    this.loadingExisting = true;
    this.service.getById(id).subscribe({
      next: (template) => {
        this.existingTemplate = template;
        this.formData = {
          form_type: template.formType,
          name: template.name,
          description: template.description || '',
          html_content: template.htmlContent,
        };
        this.loadingExisting = false;
      },
      error: () => {
        this.snackBar.open('Error cargando template', 'Cerrar', { duration: 3000 });
        this.router.navigate(['/admin/form_templates']);
      },
    });
  }

  onSubmit() {
    this.error = '';
    this.submitting = true;

    if (this.isEditMode && this.existingTemplate) {
      const updateData: UpdateFormTemplateRequest = {
        name: this.formData.name,
        description: this.formData.description || undefined,
        html_content: this.formData.html_content,
      };

      this.service.update(this.existingTemplate.id, updateData).subscribe({
        next: () => {
          this.snackBar.open('Template actualizado correctamente', 'OK', { duration: 3000 });
          this.router.navigate(['/admin/form_templates']);
        },
        error: (err) => {
          this.error = err?.error?.message || 'Error al actualizar template';
          this.submitting = false;
        },
      });
    } else {
      this.service.create(this.formData).subscribe({
        next: () => {
          this.snackBar.open('Template creado correctamente', 'OK', { duration: 3000 });
          this.router.navigate(['/admin/form_templates']);
        },
        error: (err) => {
          if (err?.status === 409) {
            this.error = `Ya existe un template con form_type "${this.formData.form_type}"`;
          } else {
            this.error = err?.error?.message || 'Error al crear template';
          }
          this.submitting = false;
        },
      });
    }
  }
}

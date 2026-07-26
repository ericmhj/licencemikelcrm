import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FormTemplateService, FormTemplate } from '../../../core/services/form-template.service';

@Component({
  selector: 'app-form-template-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatCardModule,
    MatSnackBarModule,
    MatSlideToggleModule,
    MatTooltipModule,
  ],
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title>Formularios Padre</mat-card-title>
        <mat-card-subtitle>Catálogo de templates de formularios para tenants</mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        <div class="actions-bar">
          <a mat-raised-button color="primary" routerLink="nuevo">
            <mat-icon>add</mat-icon> Nuevo Template
          </a>
        </div>

        @if (loading) {
          <p class="loading-text">Cargando templates...</p>
        } @else if (templates.length === 0) {
          <p class="empty-text">No hay templates registrados.</p>
        } @else {
          <table mat-table [dataSource]="templates" class="full-width">
            <ng-container matColumnDef="formType">
              <th mat-header-cell *matHeaderCellDef>Tipo</th>
              <td mat-cell *matCellDef="let t">
                <mat-chip-set>
                  <mat-chip>{{ t.formType }}</mat-chip>
                </mat-chip-set>
              </td>
            </ng-container>

            <ng-container matColumnDef="name">
              <th mat-header-cell *matHeaderCellDef>Nombre</th>
              <td mat-cell *matCellDef="let t">{{ t.name }}</td>
            </ng-container>

            <ng-container matColumnDef="description">
              <th mat-header-cell *matHeaderCellDef>Descripción</th>
              <td mat-cell *matCellDef="let t">{{ t.description || '—' }}</td>
            </ng-container>

            <ng-container matColumnDef="version">
              <th mat-header-cell *matHeaderCellDef>Versión</th>
              <td mat-cell *matCellDef="let t">v{{ t.currentVersion }}</td>
            </ng-container>

            <ng-container matColumnDef="sections">
              <th mat-header-cell *matHeaderCellDef>Secciones</th>
              <td mat-cell *matCellDef="let t">
                {{ t.fieldsMetadata?.sections?.length || 0 }}
              </td>
            </ng-container>

            <ng-container matColumnDef="isActive">
              <th mat-header-cell *matHeaderCellDef>Activo</th>
              <td mat-cell *matCellDef="let t">
                <mat-slide-toggle
                  [checked]="t.isActive"
                  (change)="toggleActive(t)"
                  color="primary"
                  matTooltip="Activar/Desactivar template">
                </mat-slide-toggle>
              </td>
            </ng-container>

            <ng-container matColumnDef="acciones">
              <th mat-header-cell *matHeaderCellDef>Acciones</th>
              <td mat-cell *matCellDef="let t">
                <a mat-icon-button [routerLink]="['editar', t.id]" matTooltip="Editar">
                  <mat-icon>edit</mat-icon>
                </a>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
          </table>
        }
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .actions-bar { margin-bottom: 16px; }
    .full-width { width: 100%; }
    .loading-text, .empty-text { color: #666; padding: 24px 0; text-align: center; }
    mat-chip { font-size: 12px; }
  `],
})
export class FormTemplateListComponent implements OnInit {
  private service = inject(FormTemplateService);
  private snackBar = inject(MatSnackBar);

  templates: FormTemplate[] = [];
  loading = true;
  displayedColumns = ['formType', 'name', 'description', 'version', 'sections', 'isActive', 'acciones'];

  ngOnInit() {
    this.loadTemplates();
  }

  loadTemplates() {
    this.loading = true;
    this.service.getAll().subscribe({
      next: (data) => {
        this.templates = data;
        this.loading = false;
      },
      error: () => {
        this.snackBar.open('Error cargando templates', 'Cerrar', { duration: 3000 });
        this.loading = false;
      },
    });
  }

  toggleActive(template: FormTemplate) {
    this.service.toggle(template.id).subscribe({
      next: (updated) => {
        const idx = this.templates.findIndex(t => t.id === updated.id);
        if (idx >= 0) this.templates[idx] = updated;
        this.snackBar.open(
          updated.isActive ? 'Template activado' : 'Template desactivado',
          'OK',
          { duration: 3000 },
        );
      },
      error: () => this.snackBar.open('Error al cambiar estado', 'Cerrar', { duration: 3000 }),
    });
  }
}

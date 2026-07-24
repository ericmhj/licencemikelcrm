import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FuncionRolesService } from '../../../core/services/funcion-roles.service';
import { FuncionRoles } from '../../../core/models/funcion-roles.model';

@Component({
  selector: 'app-funcion-roles-list',
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
    MatTooltipModule,
  ],
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title>Administración de Funcion-Roles</mat-card-title>
        <mat-card-subtitle>Gestión de relaciones función-roles</mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        <div class="actions-bar">
          <a mat-raised-button color="primary" routerLink="/admin/funcion_roles/nuevo">
            <mat-icon>add</mat-icon> Nueva Funcion-Roles
          </a>
        </div>

        <table mat-table [dataSource]="funcionRoles" class="full-width">
          <ng-container matColumnDef="codigo">
            <th mat-header-cell *matHeaderCellDef>Codigo</th>
            <td mat-cell *matCellDef="let item">{{ item.codigo }}</td>
          </ng-container>

          <ng-container matColumnDef="descripcion">
            <th mat-header-cell *matHeaderCellDef>Descripcion</th>
            <td mat-cell *matCellDef="let item">{{ item.descripcion }}</td>
          </ng-container>

          <ng-container matColumnDef="funcionalidad">
            <th mat-header-cell *matHeaderCellDef>Funcionalidad</th>
            <td mat-cell *matCellDef="let item">{{ item.funcionalidad }}</td>
          </ng-container>

          <ng-container matColumnDef="roles">
            <th mat-header-cell *matHeaderCellDef>Roles</th>
            <td mat-cell *matCellDef="let item">
              <mat-chip-set>
                @for (role of item.roles; track role) {
                  <mat-chip>{{ role }}</mat-chip>
                }
              </mat-chip-set>
            </td>
          </ng-container>

          <ng-container matColumnDef="acciones">
            <th mat-header-cell *matHeaderCellDef>Acciones</th>
            <td mat-cell *matCellDef="let item">
              <button mat-icon-button (click)="navigateToEdit(item)" matTooltip="Editar">
                <mat-icon>edit</mat-icon>
              </button>
              <button mat-icon-button color="warn" (click)="deleteItem(item)" matTooltip="Eliminar">
                <mat-icon>delete</mat-icon>
              </button>
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
        </table>
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .actions-bar { margin-bottom: 16px; }
    .full-width { width: 100%; }
    mat-chip { font-size: 12px; }
  `]
})
export class FuncionRolesListComponent implements OnInit {
  private readonly funcionRolesService = inject(FuncionRolesService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);

  funcionRoles: FuncionRoles[] = [];
  displayedColumns = ['codigo', 'descripcion', 'funcionalidad', 'roles', 'acciones'];

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.funcionRolesService.getAll().subscribe({
      next: (data) => this.funcionRoles = data,
      error: () => this.snackBar.open('Error cargando funcion-roles', 'Cerrar', { duration: 3000 })
    });
  }

  navigateToEdit(item: FuncionRoles): void {
    this.router.navigate(['/admin/funcion_roles/editar', item.codigo]).catch(() => {
      this.snackBar.open('Error al navegar a la edición', 'Cerrar', { duration: 3000 });
    });
  }

  deleteItem(item: FuncionRoles): void {
    if (confirm(`¿Eliminar la relación funcion-roles "${item.codigo}"?`)) {
      this.funcionRolesService.delete(item.codigo).subscribe({
        next: () => {
          this.snackBar.open('Funcion-Roles eliminada', 'OK', { duration: 3000 });
          this.loadData();
        },
        error: (err) => {
          const message = err?.message || 'Error al eliminar';
          this.snackBar.open(message, 'Cerrar', { duration: 5000 });
        }
      });
    }
  }
}

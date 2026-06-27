/**
 * Commonly used Angular Material modules for standalone component imports.
 * Import individual modules as needed in each component.
 */
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatRadioModule } from '@angular/material/radio';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';

export const MATERIAL_IMPORTS = [
  MatButtonModule,
  MatCardModule,
  MatCheckboxModule,
  MatRadioModule,
  MatIconModule,
  MatToolbarModule,
  MatSnackBarModule,
  MatProgressBarModule,
  MatTableModule,
  MatTooltipModule,
  MatChipsModule,
] as const;

// Re-export individual modules for convenience
export {
  MatButtonModule,
  MatCardModule,
  MatCheckboxModule,
  MatRadioModule,
  MatIconModule,
  MatToolbarModule,
  MatSnackBarModule,
  MatProgressBarModule,
  MatTableModule,
  MatTooltipModule,
  MatChipsModule,
};

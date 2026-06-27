import { Component, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { CreditResponse } from '../../../../core/models/api-responses.model';

export interface MonthConsumption {
  mes: string;
  creditos: number;
}

@Component({
  selector: 'app-history-table',
  standalone: true,
  imports: [CommonModule, MatTableModule],
  template: `
    <div class="bg-white rounded-lg shadow p-6">
      <h2 class="text-lg font-semibold mb-4">Historial de Consumo (6 meses)</h2>
      <table mat-table [dataSource]="historyData()" class="w-full">
        <ng-container matColumnDef="mes">
          <th mat-header-cell *matHeaderCellDef>Mes</th>
          <td mat-cell *matCellDef="let row">{{ row.mes }}</td>
        </ng-container>
        <ng-container matColumnDef="creditos">
          <th mat-header-cell *matHeaderCellDef>Créditos consumidos</th>
          <td mat-cell *matCellDef="let row">{{ row.creditos }}</td>
        </ng-container>
        <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
        <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
      </table>
    </div>
  `,
})
export class HistoryTableComponent {
  creditData = input.required<CreditResponse | null>();

  readonly displayedColumns = ['mes', 'creditos'];

  historyData = computed((): MonthConsumption[] => {
    const data = this.creditData();
    const months = this.generateLast6Months();

    if (!data) {
      return months.map(mes => ({ mes, creditos: 0 }));
    }

    // Use contadorGlobalConsultas distributed or just show 0 since API doesn't provide monthly breakdown
    // For now, show placeholder data based on available info
    return months.map((mes, index) => ({
      mes,
      creditos: index === 0 ? data.contadorGlobalConsultas : 0,
    }));
  });

  private generateLast6Months(): string[] {
    const months: string[] = [];
    const now = new Date();
    const monthNames = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];

    for (let i = 0; i < 6; i++) {
      const date = new Date(now.getFullYear(), now.getMonth() - i, 1);
      const monthLabel = `${monthNames[date.getMonth()]}-${String(date.getFullYear()).slice(2)}`;
      months.push(monthLabel);
    }
    return months;
  }
}

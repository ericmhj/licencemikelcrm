import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'currencyEur', standalone: true })
export class CurrencyEurPipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    if (value === null || value === undefined) return '0,00 €';
    return value.toLocaleString('es-ES', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' €';
  }
}

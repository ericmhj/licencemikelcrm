import { Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-progress-bar',
  standalone: true,
  template: `
    <div class="w-full bg-gray-200 rounded-full h-4" role="progressbar"
         [attr.aria-valuenow]="percentage()" aria-valuemin="0" aria-valuemax="100"
         [attr.aria-label]="'Progreso: ' + percentage() + '%'">
      <div class="h-4 rounded-full transition-all duration-300"
           [style.width.%]="clampedPercentage()"
           [class]="colorClass()">
      </div>
    </div>
  `,
})
export class ProgressBarComponent {
  percentage = input.required<number>();
  color = input.required<'green' | 'yellow' | 'red'>();

  clampedPercentage = computed(() => Math.min(this.percentage(), 100));

  colorClass = computed(() => {
    switch (this.color()) {
      case 'green': return 'bg-success';
      case 'yellow': return 'bg-warning';
      case 'red': return 'bg-danger';
    }
  });
}

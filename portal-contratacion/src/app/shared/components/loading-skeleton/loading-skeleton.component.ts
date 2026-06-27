import { Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-loading-skeleton',
  standalone: true,
  template: `
    <div class="animate-pulse space-y-3">
      @for (line of lines(); track line.index) {
        <div class="h-4 bg-gray-200 rounded" [style.width.%]="line.width"></div>
      }
    </div>
  `,
})
export class LoadingSkeletonComponent {
  linesCount = input<number>(4);

  lines = computed(() =>
    Array.from({ length: this.linesCount() }, (_, i) => ({
      index: i,
      width: 80 + (((i * 17 + 3) % 20)),
    }))
  );
}

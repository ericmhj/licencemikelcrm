import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="not-found-container">
      <h1>404</h1>
      <p>Liga desconocida</p>
      <a routerLink="/contratacion">Volver al inicio</a>
    </div>
  `,
  styles: [`
    .not-found-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      min-height: 60vh;
      text-align: center;
      padding: 32px;
    }
    h1 {
      font-size: 72px;
      font-weight: 700;
      color: #9ca3af;
      margin: 0;
    }
    p {
      font-size: 18px;
      color: #6b7280;
      margin: 12px 0 24px;
    }
    a {
      color: #1976d2;
      text-decoration: underline;
    }
    a:hover {
      text-decoration: none;
    }
  `],
})
export class NotFoundComponent {}

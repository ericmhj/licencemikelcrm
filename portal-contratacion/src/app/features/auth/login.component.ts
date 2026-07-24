import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthService } from '../../core/services/auth.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
  ],
  template: `
    <div class="login-container">
      <mat-card class="login-card">
        <mat-card-header>
          <mat-card-title>Administrador de CRM</mat-card-title>
          <mat-card-subtitle>Acceso exclusivo para administradores de plataforma</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <form (ngSubmit)="login()" class="login-form">
            @if (error()) {
              <div class="error-message">{{ error() }}</div>
            }
            <div class="field">
              <label for="email">Correo electrónico</label>
              <input
                id="email"
                type="email"
                [(ngModel)]="email"
                name="email"
                required
                placeholder="admin@mikel-crm.local"
                [disabled]="loading()"
              />
            </div>
            <div class="field">
              <label for="password">Contraseña</label>
              <input
                id="password"
                type="password"
                [(ngModel)]="password"
                name="password"
                required
                [disabled]="loading()"
              />
            </div>
            <button
              mat-raised-button
              color="primary"
              type="submit"
              [disabled]="loading() || !email || !password"
              class="login-btn"
            >
              @if (loading()) {
                Autenticando...
              } @else {
                Iniciar sesión
              }
            </button>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .login-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      background: linear-gradient(135deg, #1e3a5f 0%, #2d5a87 100%);
    }
    .login-card {
      width: 100%;
      max-width: 400px;
      padding: 24px;
    }
    .login-form {
      display: flex;
      flex-direction: column;
      gap: 16px;
      margin-top: 16px;
    }
    .field {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    .field label {
      font-weight: 500;
      font-size: 14px;
      color: #333;
    }
    .field input {
      padding: 10px 12px;
      border: 1px solid #ccc;
      border-radius: 4px;
      font-size: 14px;
    }
    .field input:focus {
      outline: none;
      border-color: #1976d2;
      box-shadow: 0 0 0 2px rgba(25, 118, 210, 0.2);
    }
    .login-btn {
      margin-top: 8px;
      padding: 12px;
    }
    .error-message {
      background: #fef2f2;
      color: #dc2626;
      padding: 10px 12px;
      border-radius: 4px;
      font-size: 13px;
    }
  `],
})
export class LoginComponent {
  private http = inject(HttpClient);
  private router = inject(Router);
  private authService = inject(AuthService);
  private snackBar = inject(MatSnackBar);

  email = '';
  password = '';
  loading = signal(false);
  error = signal<string | null>(null);

  login(): void {
    this.loading.set(true);
    this.error.set(null);

    const body = new URLSearchParams();
    body.set('grant_type', 'password');
    body.set('client_id', environment.keycloak.clientId);
    body.set('client_secret', environment.keycloak.clientSecret);
    body.set('username', this.email);
    body.set('password', this.password);

    const headers = new HttpHeaders({
      'Content-Type': 'application/x-www-form-urlencoded',
    });

    this.http
      .post<{ access_token: string; refresh_token: string }>(
        environment.keycloak.tokenUrl,
        body.toString(),
        { headers },
      )
      .subscribe({
        next: (response) => {
          this.authService.login(response.access_token);

          const role = this.authService.getUserRole();
          if (role === 'platform_admin' || role === 'superusuario' || role === 'admin') {
            this.router.navigate(['/admin/planes']);
          } else {
            this.authService.logout();
            this.error.set('No tiene permisos de administrador de plataforma');
          }
          this.loading.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          if (err.status === 401 || err.status === 400) {
            this.error.set('Credenciales inválidas');
          } else {
            this.error.set('Error de conexión con el servidor de autenticación');
          }
        },
      });
  }
}

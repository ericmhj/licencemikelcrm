import { Component, inject, computed, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet, NavigationError } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatListModule,
    MatSidenavModule,
    MatTooltipModule,
  ],
  template: `
    <div class="admin-layout">
      <mat-toolbar color="primary" class="admin-header">
        <span class="header-title">Administrador de CRM</span>
        <span class="spacer"></span>
        <button mat-icon-button (click)="logout()" aria-label="Cerrar sesión" matTooltip="Cerrar sesión">
          <mat-icon>logout</mat-icon>
        </button>
      </mat-toolbar>

      <div class="admin-content">
        <nav class="admin-sidebar">
          <mat-nav-list>
            <a mat-list-item
               routerLink="/admin/planes"
               routerLinkActive="active-link"
               [routerLinkActiveOptions]="{ paths: 'subset', queryParams: 'ignored', fragment: 'ignored', matrixParams: 'ignored' }">
              <mat-icon matListItemIcon>inventory_2</mat-icon>
              <span matListItemTitle>Planes</span>
            </a>
            <a mat-list-item
               routerLink="/admin/funcion_roles"
               routerLinkActive="active-link"
               [routerLinkActiveOptions]="{ paths: 'subset', queryParams: 'ignored', fragment: 'ignored', matrixParams: 'ignored' }">
              <mat-icon matListItemIcon>admin_panel_settings</mat-icon>
              <span matListItemTitle>Funcion-Roles</span>
            </a>
            @if (isPlatformAdmin()) {
              <a mat-list-item
                 routerLink="/admin/cartera"
                 routerLinkActive="active-link"
                 [routerLinkActiveOptions]="{ paths: 'subset', queryParams: 'ignored', fragment: 'ignored', matrixParams: 'ignored' }">
                <mat-icon matListItemIcon>account_balance_wallet</mat-icon>
                <span matListItemTitle>Cartera</span>
              </a>
              <a mat-list-item
                 routerLink="/admin/estados-cuenta"
                 routerLinkActive="active-link"
                 [routerLinkActiveOptions]="{ paths: 'subset', queryParams: 'ignored', fragment: 'ignored', matrixParams: 'ignored' }">
                <mat-icon matListItemIcon>receipt_long</mat-icon>
                <span matListItemTitle>Estados de Cuenta</span>
              </a>
            }
          </mat-nav-list>
        </nav>

        <main class="admin-main">
          <router-outlet></router-outlet>
        </main>
      </div>
    </div>
  `,
  styles: [`
    .admin-layout {
      display: flex;
      flex-direction: column;
      height: 100vh;
    }

    .admin-header {
      position: sticky;
      top: 0;
      z-index: 100;
    }

    .header-title {
      font-size: 18px;
      font-weight: 500;
    }

    .spacer {
      flex: 1 1 auto;
    }

    .admin-content {
      display: flex;
      flex: 1;
      overflow: hidden;
    }

    .admin-sidebar {
      width: 240px;
      min-width: 240px;
      background: #fafafa;
      border-right: 1px solid #e0e0e0;
      overflow-y: auto;
    }

    .admin-main {
      flex: 1;
      padding: 24px;
      overflow-y: auto;
      background: #fff;
    }

    .active-link {
      background: rgba(25, 118, 210, 0.08) !important;
      color: #1976d2;
    }
  `],
})
export class AdminLayoutComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private router = inject(Router);
  private navigationErrorSub?: Subscription;

  /** Solo muestra la liga de cartera si el rol es platform_admin */
  isPlatformAdmin = computed(() => this.authService.getUserRole() === 'platform_admin');

  ngOnInit(): void {
    this.navigationErrorSub = this.router.events
      .pipe(filter((event): event is NavigationError => event instanceof NavigationError))
      .subscribe((event: NavigationError) => {
        // Fallback: full page reload if Angular Router navigation fails
        window.location.href = event.url;
      });
  }

  ngOnDestroy(): void {
    this.navigationErrorSub?.unsubscribe();
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']).catch(() => {
      window.location.href = '/login';
    });
  }
}

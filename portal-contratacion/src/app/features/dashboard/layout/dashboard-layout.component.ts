import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { DashboardStateService } from '../services/dashboard-state.service';
import { AuthService } from '../../../core/services/auth.service';
import { ContractSectionComponent } from '../components/contract-section/contract-section.component';
import { ModulesSectionComponent } from '../components/modules-section/modules-section.component';
import { CreditsSectionComponent } from '../components/credits-section/credits-section.component';
import { QuotaSectionComponent } from '../components/quota-section/quota-section.component';
import { HistoryTableComponent } from '../components/history-table/history-table.component';
import { ActionButtonsComponent } from '../components/action-buttons/action-buttons.component';
import { BannerPromoComponent } from '../components/banner-promo/banner-promo.component';
import { LoadingSkeletonComponent } from '../../../shared/components/loading-skeleton/loading-skeleton.component';
import { ErrorHandlerComponent } from '../../../shared/components/error-handler/error-handler.component';

@Component({
  selector: 'app-dashboard-layout',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    ContractSectionComponent,
    ModulesSectionComponent,
    CreditsSectionComponent,
    QuotaSectionComponent,
    HistoryTableComponent,
    ActionButtonsComponent,
    BannerPromoComponent,
    LoadingSkeletonComponent,
    ErrorHandlerComponent,
  ],
  providers: [DashboardStateService],
  template: `
    <div class="min-h-screen bg-[var(--color-bg-page)]">
      <header class="bg-white shadow-sm border-b">
        <div class="max-w-7xl mx-auto px-4 py-4 flex items-center justify-between">
          <div class="flex items-center gap-3">
            <h1 class="text-xl font-medium text-[var(--color-primary)]">Mikel CRM</h1>
            <span class="text-gray-500">|</span>
            <span class="text-gray-700">Dashboard</span>
            @if (tenantId()) {
              <span class="text-gray-500">—</span>
              <span class="text-sm text-gray-600">{{ tenantId() }}</span>
            }
          </div>
          <button mat-stroked-button color="warn" (click)="logout()" class="min-h-[44px]">
            Cerrar sesión
          </button>
        </div>
      </header>

      <main class="max-w-7xl mx-auto px-4 py-8">
        <!-- Banners -->
        <app-banner-promo
          [contractData]="state.contractData()"
          [creditData]="state.creditData()" />

        <!-- Grid layout: 2 columns desktop, 1 column mobile -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <!-- Contract Section -->
          @if (state.contractLoading()) {
            <app-loading-skeleton [linesCount]="5" />
          } @else if (state.contractError()) {
            <app-error-handler [error]="state.contractError()!" (retry)="state.retrySection('contracts')" />
          } @else {
            <app-contract-section
              [contractData]="state.contractData()"
              [loading]="state.contractLoading()"
              [error]="state.contractError()" />
          }

          <!-- Credits Section -->
          @if (state.creditsLoading()) {
            <app-loading-skeleton [linesCount]="5" />
          } @else if (state.creditsError()) {
            <app-error-handler [error]="state.creditsError()!" (retry)="state.retrySection('credits')" />
          } @else {
            <app-credits-section
              [creditData]="state.creditData()"
              [loading]="state.creditsLoading()"
              [error]="state.creditsError()" />
          }

          <!-- Modules Section -->
          <app-modules-section
            [contractData]="state.contractData()"
            [accessData]="state.accessData()" />

          <!-- Quota Section -->
          <app-quota-section
            [contractData]="state.contractData()" />
        </div>

        <!-- History Table (full width) -->
        <div class="mt-6">
          <app-history-table [creditData]="state.creditData()" />
        </div>

        <!-- Action Buttons -->
        <div class="mt-6">
          <app-action-buttons />
        </div>
      </main>
    </div>
  `,
})
export class DashboardLayoutComponent implements OnInit {
  readonly state = inject(DashboardStateService);
  private readonly authService = inject(AuthService);

  readonly tenantId = this.authService.tenantId;

  ngOnInit(): void {
    this.state.loadAllSections();
  }

  logout(): void {
    this.authService.logout();
  }
}

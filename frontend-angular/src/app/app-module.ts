import { NgModule, provideBrowserGlobalErrorListeners } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';

import { AppRoutingModule } from './app-routing-module';
import { App } from './app';
import { ClaimCheckerComponent } from './pages/claim-checker/claim-checker.component';
import { ReportFormComponent } from './pages/report-form/report-form.component';
import { VerifiedClaimsComponent } from './pages/verified-claims/verified-claims.component';
import { OfficialInfoComponent } from './pages/official-info/official-info.component';
import { ReportStatusComponent } from './pages/report-status/report-status.component';
import { ValidatorDashboardComponent } from './pages/validator-dashboard/validator-dashboard.component';
import { ValidatorReportDetailComponent } from './pages/validator-report-detail/validator-report-detail.component';

@NgModule({
  declarations: [
    App,
    ClaimCheckerComponent,
    ReportFormComponent,
    VerifiedClaimsComponent,
    OfficialInfoComponent,
    ReportStatusComponent,
    ValidatorDashboardComponent,
    ValidatorReportDetailComponent
  ],
  imports: [
    BrowserModule,
    FormsModule,
    HttpClientModule,
    AppRoutingModule
  ],
  providers: [
    provideBrowserGlobalErrorListeners(),
  ],
  bootstrap: [App]
})
export class AppModule { }

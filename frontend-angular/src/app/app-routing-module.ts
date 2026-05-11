import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ClaimCheckerComponent } from './pages/claim-checker/claim-checker.component';
import { OfficialInfoComponent } from './pages/official-info/official-info.component';
import { ReportFormComponent } from './pages/report-form/report-form.component';
import { ReportStatusComponent } from './pages/report-status/report-status.component';
import { ValidatorDashboardComponent } from './pages/validator-dashboard/validator-dashboard.component';
import { ValidatorReportDetailComponent } from './pages/validator-report-detail/validator-report-detail.component';
import { VerifiedClaimsComponent } from './pages/verified-claims/verified-claims.component';

const routes: Routes = [
  { path: '', component: ClaimCheckerComponent },
  { path: 'report', component: ReportFormComponent },
  { path: 'verified-claims', component: VerifiedClaimsComponent },
  { path: 'official-info', component: OfficialInfoComponent },
  { path: 'report-status', component: ReportStatusComponent },
  { path: 'validator', component: ValidatorDashboardComponent },
  { path: 'validator/reports/:id', component: ValidatorReportDetailComponent },
  { path: '**', redirectTo: '' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }

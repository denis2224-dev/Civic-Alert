import { ChangeDetectorRef, Component } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize, timeout, TimeoutError } from 'rxjs';
import { ReportStatusResponse } from '../../models/report.model';
import { ReportService } from '../../services/report.service';
import { toDisplayLabel } from '../../utils/display-label';

@Component({
  selector: 'app-report-status',
  standalone: false,
  templateUrl: './report-status.component.html',
  styleUrl: './report-status.component.scss'
})
export class ReportStatusComponent {
  private static readonly LOOKUP_TIMEOUT_MS = 12000;

  trackingCode = '';
  loading = false;
  errorMessage = '';
  reportStatus: ReportStatusResponse | null = null;

  constructor(
    private readonly reportService: ReportService,
    private readonly changeDetectorRef: ChangeDetectorRef
  ) {}

  checkStatus(): void {
    if (!this.trackingCode.trim()) {
      this.errorMessage = 'Tracking code is required.';
      this.changeDetectorRef.markForCheck();
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.reportStatus = null;
    this.changeDetectorRef.markForCheck();

    this.reportService
      .getReportStatus(this.trackingCode.trim())
      .pipe(
        timeout(ReportStatusComponent.LOOKUP_TIMEOUT_MS),
        finalize(() => {
          this.loading = false;
          this.changeDetectorRef.markForCheck();
        })
      )
      .subscribe({
        next: (status) => {
          this.reportStatus = status;
          this.changeDetectorRef.markForCheck();
        },
        error: (error: unknown) => {
          console.error('Report status lookup failed:', error);
          if (error instanceof TimeoutError) {
            this.errorMessage = 'Unable to load report status right now.';
            this.changeDetectorRef.markForCheck();
            return;
          }
          if (error instanceof HttpErrorResponse && error.status === 0) {
            this.errorMessage = 'Could not connect to the backend. Make sure Spring Boot is running on port 8080.';
            this.changeDetectorRef.markForCheck();
            return;
          }
          this.errorMessage = 'Tracking code not found.';
          this.changeDetectorRef.markForCheck();
        }
      });
  }

  formatLabel(value?: string | null): string {
    return toDisplayLabel(value);
  }

  riskClass(value?: string | null): string {
    if (!value) {
      return 'risk-low';
    }
    return `risk-${value.toLowerCase()}`;
  }
}

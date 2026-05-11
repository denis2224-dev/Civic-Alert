import { Component } from '@angular/core';
import { ReportStatusResponse } from '../../models/report.model';
import { ReportService } from '../../services/report.service';

@Component({
  selector: 'app-report-status',
  standalone: false,
  templateUrl: './report-status.component.html',
  styleUrl: './report-status.component.scss'
})
export class ReportStatusComponent {
  trackingCode = '';
  loading = false;
  errorMessage = '';
  reportStatus: ReportStatusResponse | null = null;

  constructor(private readonly reportService: ReportService) {}

  checkStatus(): void {
    if (!this.trackingCode.trim()) {
      this.errorMessage = 'Tracking code is required.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.reportStatus = null;

    this.reportService.getReportStatus(this.trackingCode.trim()).subscribe({
      next: (status) => {
        this.reportStatus = status;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Tracking code not found.';
        this.loading = false;
      }
    });
  }
}


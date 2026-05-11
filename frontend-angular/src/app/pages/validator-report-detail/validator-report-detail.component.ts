import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ClaimStatus } from '../../models/claim-check.model';
import { ValidatorDetectionLog, ValidatorReport } from '../../models/validator.model';
import { ValidatorService } from '../../services/validator.service';

@Component({
  selector: 'app-validator-report-detail',
  standalone: false,
  templateUrl: './validator-report-detail.component.html',
  styleUrl: './validator-report-detail.component.scss'
})
export class ValidatorReportDetailComponent implements OnInit {
  reportId: number | null = null;
  report: ValidatorReport | null = null;
  detectionLogs: ValidatorDetectionLog[] = [];
  correctionText = '';
  officialSource = 'Central Electoral Commission';
  officialSourceUrl = '';
  publish = true;
  loading = false;
  errorMessage = '';
  successMessage = '';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly validatorService: ValidatorService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const idParam = params.get('id');
      this.reportId = idParam ? Number(idParam) : null;
      if (this.reportId) {
        this.loadDetails();
      }
    });
  }

  loadDetails(): void {
    if (!this.reportId) {
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.validatorService.getReportDetails(this.reportId).subscribe({
      next: (details) => {
        this.report = details.report;
        this.detectionLogs = details.detectionLogs;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Unable to load report details.';
        this.loading = false;
      }
    });
  }

  markUnderReview(): void {
    if (!this.reportId) {
      return;
    }

    this.validatorService.markUnderReview(this.reportId).subscribe({
      next: (report) => {
        this.report = report;
        this.successMessage = 'Report marked as UNDER_REVIEW.';
        this.errorMessage = '';
      },
      error: () => {
        this.errorMessage = 'Unable to update report status.';
      }
    });
  }

  submitDecision(status: ClaimStatus): void {
    if (!this.reportId) {
      return;
    }

    this.validatorService
      .submitDecision(this.reportId, {
        status,
        correctionText: this.correctionText.trim() || undefined,
        officialSource: this.officialSource.trim() || undefined,
        officialSourceUrl: this.officialSourceUrl.trim() || undefined,
        publish: this.publish
      })
      .subscribe({
        next: (report) => {
          this.report = report;
          this.successMessage = `Decision saved: ${status}`;
          this.errorMessage = '';
          this.loadDetails();
        },
        error: () => {
          this.errorMessage = 'Unable to save validator decision.';
        }
      });
  }
}


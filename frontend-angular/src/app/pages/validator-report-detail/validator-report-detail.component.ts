import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { finalize, timeout, TimeoutError } from 'rxjs';
import { ClaimStatus } from '../../models/claim-check.model';
import { ValidatorDetectionLog, ValidatorReport } from '../../models/validator.model';
import { ValidatorService } from '../../services/validator.service';
import { toDisplayLabel } from '../../utils/display-label';

@Component({
  selector: 'app-validator-report-detail',
  standalone: false,
  templateUrl: './validator-report-detail.component.html',
  styleUrl: './validator-report-detail.component.scss'
})
export class ValidatorReportDetailComponent implements OnInit {
  private static readonly REQUEST_TIMEOUT_MS = 12000;

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
    private readonly validatorService: ValidatorService,
    private readonly changeDetectorRef: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const idParam = params.get('id');
      this.reportId = idParam ? Number(idParam) : null;
      if (this.reportId) {
        this.loadDetails();
      }
      this.changeDetectorRef.markForCheck();
    });
  }

  loadDetails(): void {
    if (!this.reportId) {
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.changeDetectorRef.markForCheck();

    this.validatorService
      .getReportDetails(this.reportId)
      .pipe(
        timeout(ValidatorReportDetailComponent.REQUEST_TIMEOUT_MS),
        finalize(() => {
          this.loading = false;
          this.changeDetectorRef.markForCheck();
        })
      )
      .subscribe({
        next: (details) => {
          this.report = details.report;
          this.detectionLogs = details.detectionLogs;
          this.changeDetectorRef.markForCheck();
        },
        error: (error: unknown) => {
          this.errorMessage = this.resolveApiError(error, 'Unable to load report details.');
          this.changeDetectorRef.markForCheck();
        }
      });
  }

  markUnderReview(): void {
    if (!this.reportId) {
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.changeDetectorRef.markForCheck();

    this.validatorService
      .markUnderReview(this.reportId)
      .pipe(
        timeout(ValidatorReportDetailComponent.REQUEST_TIMEOUT_MS),
        finalize(() => {
          this.loading = false;
          this.changeDetectorRef.markForCheck();
        })
      )
      .subscribe({
        next: (report) => {
          this.report = report;
          this.successMessage = 'Report marked as UNDER_REVIEW.';
          this.errorMessage = '';
          this.changeDetectorRef.markForCheck();
        },
        error: (error: unknown) => {
          this.errorMessage = this.resolveApiError(error, 'Unable to update report status.');
          this.changeDetectorRef.markForCheck();
        }
      });
  }

  submitDecision(status: ClaimStatus): void {
    if (!this.reportId) {
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.changeDetectorRef.markForCheck();

    this.validatorService
      .submitDecision(this.reportId, {
        status,
        correctionText: this.correctionText.trim() || undefined,
        officialSource: this.officialSource.trim() || undefined,
        officialSourceUrl: this.officialSourceUrl.trim() || undefined,
        publish: this.publish
      })
      .pipe(
        timeout(ValidatorReportDetailComponent.REQUEST_TIMEOUT_MS),
        finalize(() => {
          this.loading = false;
          this.changeDetectorRef.markForCheck();
        })
      )
      .subscribe({
        next: (report) => {
          this.report = report;
          this.successMessage = `Decision saved: ${status}`;
          this.errorMessage = '';
          this.changeDetectorRef.markForCheck();
          this.loadDetails();
        },
        error: (error: unknown) => {
          this.errorMessage = this.resolveApiError(error, 'Unable to save validator decision.');
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

  private resolveApiError(error: unknown, defaultMessage: string): string {
    console.error('Validator API request failed:', error);
    if (error instanceof TimeoutError) {
      return 'The request took too long. Please try again.';
    }
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return 'Could not connect to the backend. Make sure Spring Boot is running on port 8080.';
    }
    if (error instanceof HttpErrorResponse && typeof error.error?.message === 'string' && error.error.message.trim()) {
      return error.error.message;
    }
    return defaultMessage;
  }
}

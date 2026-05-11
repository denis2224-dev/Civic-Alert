import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { finalize, timeout, TimeoutError } from 'rxjs';
import { ReportResponse } from '../../models/report.model';
import { ReportService } from '../../services/report.service';
import { toDisplayLabel } from '../../utils/display-label';

@Component({
  selector: 'app-report-form',
  standalone: false,
  templateUrl: './report-form.component.html',
  styleUrl: './report-form.component.scss'
})
export class ReportFormComponent implements OnInit {
  private static readonly SUBMIT_TIMEOUT_MS = 12000;
  private static readonly FAILSAFE_LOADING_RESET_MS = 15000;

  text = '';
  platform = 'Telegram';
  region = 'Chisinau';
  sourceUrl = '';
  language = 'en';
  loading = false;
  errorMessage = '';
  response: ReportResponse | null = null;
  private loadingFailsafeTimer: ReturnType<typeof setTimeout> | null = null;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly reportService: ReportService,
    private readonly changeDetectorRef: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      const text = params.get('text');
      const region = params.get('region');
      const language = params.get('language');

      if (text) {
        this.text = text;
      }
      if (region) {
        this.region = region;
      }
      if (language) {
        this.language = language;
      }
      this.changeDetectorRef.markForCheck();
    });
  }

  submitReport(): void {
    if (!this.text.trim()) {
      this.errorMessage = 'Claim text is required.';
      this.changeDetectorRef.markForCheck();
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.response = null;
    this.resetLoadingFailsafe();
    this.loadingFailsafeTimer = setTimeout(() => {
      if (this.loading) {
        this.loading = false;
        this.errorMessage = 'Unable to submit report right now.';
        this.changeDetectorRef.markForCheck();
      }
    }, ReportFormComponent.FAILSAFE_LOADING_RESET_MS);

    this.reportService
      .submitReport({
        text: this.text.trim(),
        platform: this.platform,
        region: this.region.trim(),
        sourceUrl: this.sourceUrl.trim() || undefined,
        language: this.language
      })
      .pipe(
        timeout(ReportFormComponent.SUBMIT_TIMEOUT_MS),
        finalize(() => {
          this.loading = false;
          this.resetLoadingFailsafe();
          this.changeDetectorRef.markForCheck();
        })
      )
      .subscribe({
        next: (response) => {
          this.response = response;
          this.changeDetectorRef.markForCheck();
        },
        error: (error: unknown) => {
          console.error('Report submit failed:', error);

          if (error instanceof TimeoutError) {
            this.errorMessage = 'Report submission timed out. Please try again.';
            this.changeDetectorRef.markForCheck();
            return;
          }

          if (error instanceof HttpErrorResponse && error.status === 0) {
            this.errorMessage = 'Could not connect to the backend. Make sure Spring Boot is running on port 8080.';
            this.changeDetectorRef.markForCheck();
            return;
          }

          this.errorMessage = 'Unable to submit report right now.';
          this.changeDetectorRef.markForCheck();
        }
      });
  }

  formatLabel(value?: string | null): string {
    return toDisplayLabel(value);
  }

  statusClass(status?: string | null): string {
    if (!status) {
      return 'status-neutral';
    }
    return `status-${status.toLowerCase()}`;
  }

  private resetLoadingFailsafe(): void {
    if (this.loadingFailsafeTimer) {
      clearTimeout(this.loadingFailsafeTimer);
      this.loadingFailsafeTimer = null;
    }
  }
}

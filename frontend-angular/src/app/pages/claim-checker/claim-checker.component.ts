import { ChangeDetectorRef, Component } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize, timeout, TimeoutError } from 'rxjs';
import { ClaimCheckResponse } from '../../models/claim-check.model';
import { ClaimService } from '../../services/claim.service';
import { toDisplayLabel } from '../../utils/display-label';

@Component({
  selector: 'app-claim-checker',
  standalone: false,
  templateUrl: './claim-checker.component.html',
  styleUrl: './claim-checker.component.scss'
})
export class ClaimCheckerComponent {
  private static readonly CHECK_TIMEOUT_MS = 12000;
  private static readonly FAILSAFE_LOADING_RESET_MS = 15000;

  text = '';
  region = 'Chisinau';
  language = 'en';
  loading = false;
  errorMessage = '';
  result: ClaimCheckResponse | null = null;
  private loadingFailsafeTimer: ReturnType<typeof setTimeout> | null = null;

  constructor(
    private readonly claimService: ClaimService,
    private readonly changeDetectorRef: ChangeDetectorRef
  ) {}

  checkClaim(): void {
    if (!this.text.trim()) {
      this.errorMessage = 'Please enter a claim to check.';
      this.scheduleViewUpdate();
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.result = null;
    this.resetLoadingFailsafe();
    this.loadingFailsafeTimer = setTimeout(() => {
      if (this.loading) {
        this.loading = false;
        this.errorMessage = 'Something went wrong while checking the claim. Please try again.';
        this.scheduleViewUpdate();
      }
    }, ClaimCheckerComponent.FAILSAFE_LOADING_RESET_MS);

    try {
      this.claimService
        .checkClaim({
          text: this.text.trim(),
          region: this.region.trim(),
          language: this.language.trim()
        })
        .pipe(
          timeout(ClaimCheckerComponent.CHECK_TIMEOUT_MS),
          finalize(() => {
            this.loading = false;
            this.resetLoadingFailsafe();
            this.scheduleViewUpdate();
          })
        )
        .subscribe({
          next: (response) => {
            this.result = response;
            this.scheduleViewUpdate();
          },
          error: (error: unknown) => {
            console.error('Claim check request failed:', error);

            if (error instanceof TimeoutError) {
              this.errorMessage = 'Something went wrong while checking the claim. Please try again.';
              this.scheduleViewUpdate();
              return;
            }

            if (error instanceof HttpErrorResponse) {
              if (error.status === 0) {
                this.errorMessage = 'Could not connect to the backend. Make sure Spring Boot is running on port 8080.';
                this.scheduleViewUpdate();
                return;
              }
              this.errorMessage = 'Something went wrong while checking the claim. Please try again.';
              this.scheduleViewUpdate();
              return;
            }

            this.errorMessage = 'Something went wrong while checking the claim. Please try again.';
            this.scheduleViewUpdate();
          }
        });
    } catch (error) {
      console.error('Claim check setup failed:', error);
      this.loading = false;
      this.resetLoadingFailsafe();
      this.errorMessage = 'Something went wrong while checking the claim. Please try again.';
      this.scheduleViewUpdate();
    }
  }

  get showReportButton(): boolean {
    return this.result?.status === 'NEEDS_REVIEW' || this.result?.status === 'NO_MATCH_FOUND';
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

  riskClass(riskLevel?: string | null): string {
    if (!riskLevel) {
      return 'risk-low';
    }
    return `risk-${riskLevel.toLowerCase()}`;
  }

  private resetLoadingFailsafe(): void {
    if (this.loadingFailsafeTimer) {
      clearTimeout(this.loadingFailsafeTimer);
      this.loadingFailsafeTimer = null;
    }
  }

  private scheduleViewUpdate(): void {
    this.changeDetectorRef.markForCheck();
  }
}

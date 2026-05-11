import { Component } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize, timeout, TimeoutError } from 'rxjs';
import { ClaimCheckResponse } from '../../models/claim-check.model';
import { ClaimService } from '../../services/claim.service';

@Component({
  selector: 'app-claim-checker',
  standalone: false,
  templateUrl: './claim-checker.component.html',
  styleUrl: './claim-checker.component.scss'
})
export class ClaimCheckerComponent {
  private static readonly CHECK_TIMEOUT_MS = 12000;

  text = '';
  region = 'Chisinau';
  language = 'en';
  loading = false;
  errorMessage = '';
  result: ClaimCheckResponse | null = null;

  constructor(private readonly claimService: ClaimService) {}

  checkClaim(): void {
    if (!this.text.trim()) {
      this.errorMessage = 'Please enter a claim to check.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.result = null;

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
        })
      )
      .subscribe({
        next: (response) => {
          this.result = response;
        },
        error: (error: unknown) => {
          console.error('Claim check request failed:', error);

          if (error instanceof TimeoutError) {
            this.errorMessage = 'Something went wrong while checking the claim. Please try again.';
            return;
          }

          if (error instanceof HttpErrorResponse) {
            if (error.status === 0) {
              this.errorMessage = 'Could not connect to the backend. Make sure Spring Boot is running on port 8080.';
              return;
            }
            this.errorMessage = 'Something went wrong while checking the claim. Please try again.';
            return;
          }

          this.errorMessage = 'Something went wrong while checking the claim. Please try again.';
        }
      });
  }

  get showReportButton(): boolean {
    return this.result?.status === 'NEEDS_REVIEW' || this.result?.status === 'NO_MATCH_FOUND';
  }
}

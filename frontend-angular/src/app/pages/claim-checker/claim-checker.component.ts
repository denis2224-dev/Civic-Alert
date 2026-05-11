import { Component } from '@angular/core';
import { ClaimCheckResponse } from '../../models/claim-check.model';
import { ClaimService } from '../../services/claim.service';

@Component({
  selector: 'app-claim-checker',
  standalone: false,
  templateUrl: './claim-checker.component.html',
  styleUrl: './claim-checker.component.scss'
})
export class ClaimCheckerComponent {
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
      .subscribe({
        next: (response) => {
          this.result = response;
          this.loading = false;
        },
        error: () => {
          this.errorMessage = 'Unable to check claim right now.';
          this.loading = false;
        }
      });
  }

  get showReportButton(): boolean {
    return this.result?.status === 'NEEDS_REVIEW' || this.result?.status === 'NO_MATCH_FOUND';
  }
}


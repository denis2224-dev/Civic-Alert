import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { VerifiedClaim } from '../../models/verified-claim.model';
import { VerifiedClaimService } from '../../services/verified-claim.service';
import { toDisplayLabel } from '../../utils/display-label';

@Component({
  selector: 'app-verified-claims',
  standalone: false,
  templateUrl: './verified-claims.component.html',
  styleUrl: './verified-claims.component.scss'
})
export class VerifiedClaimsComponent implements OnInit {
  claims: VerifiedClaim[] = [];
  filteredClaims: VerifiedClaim[] = [];
  categories: string[] = [];
  selectedCategory = 'all';
  errorMessage = '';

  constructor(
    private readonly verifiedClaimService: VerifiedClaimService,
    private readonly changeDetectorRef: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadClaims();
  }

  loadClaims(): void {
    this.verifiedClaimService.getVerifiedClaims(undefined, 'en').subscribe({
      next: (claims) => {
        this.claims = claims;
        this.categories = Array.from(new Set(claims.map((claim) => claim.category))).sort();
        this.applyFilter();
        this.changeDetectorRef.markForCheck();
      },
      error: () => {
        this.errorMessage = 'Unable to load verified claims.';
        this.changeDetectorRef.markForCheck();
      }
    });
  }

  applyFilter(): void {
    this.filteredClaims =
      this.selectedCategory === 'all'
        ? this.claims
        : this.claims.filter((claim) => claim.category === this.selectedCategory);
    this.changeDetectorRef.markForCheck();
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
}

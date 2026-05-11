import { Component, OnInit } from '@angular/core';
import { VerifiedClaim } from '../../models/verified-claim.model';
import { VerifiedClaimService } from '../../services/verified-claim.service';

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

  constructor(private readonly verifiedClaimService: VerifiedClaimService) {}

  ngOnInit(): void {
    this.loadClaims();
  }

  loadClaims(): void {
    this.verifiedClaimService.getVerifiedClaims(undefined, 'en').subscribe({
      next: (claims) => {
        this.claims = claims;
        this.categories = Array.from(new Set(claims.map((claim) => claim.category))).sort();
        this.applyFilter();
      },
      error: () => {
        this.errorMessage = 'Unable to load verified claims.';
      }
    });
  }

  applyFilter(): void {
    this.filteredClaims =
      this.selectedCategory === 'all'
        ? this.claims
        : this.claims.filter((claim) => claim.category === this.selectedCategory);
  }
}


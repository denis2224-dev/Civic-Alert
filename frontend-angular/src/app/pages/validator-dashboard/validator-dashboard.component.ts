import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize, timeout, TimeoutError } from 'rxjs';
import { ValidatorReport } from '../../models/validator.model';
import { ValidatorService } from '../../services/validator.service';
import { toDisplayLabel } from '../../utils/display-label';

@Component({
  selector: 'app-validator-dashboard',
  standalone: false,
  templateUrl: './validator-dashboard.component.html',
  styleUrl: './validator-dashboard.component.scss'
})
export class ValidatorDashboardComponent implements OnInit {
  private static readonly LOAD_TIMEOUT_MS = 12000;

  reports: ValidatorReport[] = [];
  loading = false;
  errorMessage = '';

  constructor(
    private readonly validatorService: ValidatorService,
    private readonly changeDetectorRef: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadReports();
  }

  loadReports(): void {
    this.loading = true;
    this.errorMessage = '';
    this.changeDetectorRef.markForCheck();
    this.validatorService
      .getReports()
      .pipe(
        timeout(ValidatorDashboardComponent.LOAD_TIMEOUT_MS),
        finalize(() => {
          this.loading = false;
          this.changeDetectorRef.markForCheck();
        })
      )
      .subscribe({
        next: (reports) => {
          this.reports = reports;
          this.changeDetectorRef.markForCheck();
        },
        error: (error: unknown) => {
          console.error('Validator reports load failed:', error);
          if (error instanceof TimeoutError) {
            this.errorMessage = 'Loading reports took too long. Please try again.';
            this.changeDetectorRef.markForCheck();
            return;
          }
          if (error instanceof HttpErrorResponse && error.status === 0) {
            this.errorMessage = 'Could not connect to the backend. Make sure Spring Boot is running on port 8080.';
            this.changeDetectorRef.markForCheck();
            return;
          }
          this.errorMessage = 'Unable to load validator reports.';
          this.changeDetectorRef.markForCheck();
        }
      });
  }

  riskClass(value?: string): string {
    if (!value) {
      return 'risk-low';
    }
    return `risk-${value.toLowerCase()}`;
  }

  formatLabel(value?: string | null): string {
    return toDisplayLabel(value);
  }
}

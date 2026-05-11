import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
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
    this.validatorService.getReports().subscribe({
      next: (reports) => {
        this.reports = reports;
        this.loading = false;
        this.changeDetectorRef.markForCheck();
      },
      error: () => {
        this.errorMessage = 'Unable to load validator reports.';
        this.loading = false;
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

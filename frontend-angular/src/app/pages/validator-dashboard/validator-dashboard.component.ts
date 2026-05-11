import { Component, OnInit } from '@angular/core';
import { ValidatorReport } from '../../models/validator.model';
import { ValidatorService } from '../../services/validator.service';

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

  constructor(private readonly validatorService: ValidatorService) {}

  ngOnInit(): void {
    this.loadReports();
  }

  loadReports(): void {
    this.loading = true;
    this.errorMessage = '';
    this.validatorService.getReports().subscribe({
      next: (reports) => {
        this.reports = reports;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Unable to load validator reports.';
        this.loading = false;
      }
    });
  }

  riskClass(value?: string): string {
    if (!value) {
      return 'risk-low';
    }
    return `risk-${value.toLowerCase()}`;
  }
}


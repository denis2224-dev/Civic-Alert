import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ReportResponse } from '../../models/report.model';
import { ReportService } from '../../services/report.service';

@Component({
  selector: 'app-report-form',
  standalone: false,
  templateUrl: './report-form.component.html',
  styleUrl: './report-form.component.scss'
})
export class ReportFormComponent implements OnInit {
  text = '';
  platform = 'Telegram';
  region = 'Chisinau';
  sourceUrl = '';
  language = 'en';
  loading = false;
  errorMessage = '';
  response: ReportResponse | null = null;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly reportService: ReportService
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
    });
  }

  submitReport(): void {
    if (!this.text.trim()) {
      this.errorMessage = 'Claim text is required.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.response = null;

    this.reportService
      .submitReport({
        text: this.text.trim(),
        platform: this.platform,
        region: this.region.trim(),
        sourceUrl: this.sourceUrl.trim() || undefined,
        language: this.language
      })
      .subscribe({
        next: (response) => {
          this.response = response;
          this.loading = false;
        },
        error: () => {
          this.errorMessage = 'Unable to submit report right now.';
          this.loading = false;
        }
      });
  }
}


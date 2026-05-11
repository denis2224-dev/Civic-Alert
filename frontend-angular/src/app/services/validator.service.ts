import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE } from './api-base';
import { ValidatorDecisionRequest, ValidatorReport, ValidatorReportDetails } from '../models/validator.model';

@Injectable({
  providedIn: 'root'
})
export class ValidatorService {
  private readonly endpoint = `${API_BASE}/validator/reports`;

  constructor(private readonly http: HttpClient) {}

  getReports(): Observable<ValidatorReport[]> {
    return this.http.get<ValidatorReport[]>(this.endpoint);
  }

  getReportDetails(reportId: number): Observable<ValidatorReportDetails> {
    return this.http.get<ValidatorReportDetails>(`${this.endpoint}/${reportId}`);
  }

  markUnderReview(reportId: number): Observable<ValidatorReport> {
    return this.http.patch<ValidatorReport>(`${this.endpoint}/${reportId}/under-review`, {});
  }

  submitDecision(reportId: number, payload: ValidatorDecisionRequest): Observable<ValidatorReport> {
    return this.http.post<ValidatorReport>(`${this.endpoint}/${reportId}/decision`, payload);
  }
}


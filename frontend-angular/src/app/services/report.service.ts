import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE } from './api-base';
import { ReportRequest, ReportResponse, ReportStatusResponse } from '../models/report.model';

@Injectable({
  providedIn: 'root'
})
export class ReportService {
  private readonly baseEndpoint = `${API_BASE}/public/reports`;

  constructor(private readonly http: HttpClient) {}

  submitReport(payload: ReportRequest): Observable<ReportResponse> {
    return this.http.post<ReportResponse>(this.baseEndpoint, payload);
  }

  getReportStatus(trackingCode: string): Observable<ReportStatusResponse> {
    return this.http.get<ReportStatusResponse>(`${this.baseEndpoint}/${trackingCode}`);
  }
}


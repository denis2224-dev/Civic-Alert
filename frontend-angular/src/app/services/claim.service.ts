import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE } from './api-base';
import { ClaimCheckRequest, ClaimCheckResponse } from '../models/claim-check.model';

@Injectable({
  providedIn: 'root'
})
export class ClaimService {
  private readonly endpoint = `${API_BASE}/public/check-claim`;

  constructor(private readonly http: HttpClient) {}

  checkClaim(payload: ClaimCheckRequest): Observable<ClaimCheckResponse> {
    return this.http.post<ClaimCheckResponse>(this.endpoint, payload);
  }
}


import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE } from './api-base';
import { VerifiedClaim } from '../models/verified-claim.model';

@Injectable({
  providedIn: 'root'
})
export class VerifiedClaimService {
  private readonly endpoint = `${API_BASE}/public/verified-claims`;

  constructor(private readonly http: HttpClient) {}

  getVerifiedClaims(category?: string, language?: string): Observable<VerifiedClaim[]> {
    let params = new HttpParams();
    if (category) {
      params = params.set('category', category);
    }
    if (language) {
      params = params.set('language', language);
    }
    return this.http.get<VerifiedClaim[]>(this.endpoint, { params });
  }
}


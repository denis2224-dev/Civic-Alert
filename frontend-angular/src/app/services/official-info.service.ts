import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE } from './api-base';
import { OfficialInfo } from '../models/official-info.model';

@Injectable({
  providedIn: 'root'
})
export class OfficialInfoService {
  private readonly endpoint = `${API_BASE}/public/official-info`;

  constructor(private readonly http: HttpClient) {}

  getOfficialInfo(): Observable<OfficialInfo[]> {
    return this.http.get<OfficialInfo[]>(this.endpoint);
  }
}


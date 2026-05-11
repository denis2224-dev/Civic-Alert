import { RiskLevel } from './claim-check.model';

export type ReportStatus =
  | 'RECEIVED'
  | 'UNDER_REVIEW'
  | 'RESOLVED'
  | 'MERGED'
  | 'REJECTED'
  | 'PUBLISHED';

export interface ReportRequest {
  text: string;
  platform: string;
  region: string;
  sourceUrl?: string;
  language: string;
}

export interface ReportResponse {
  trackingCode: string;
  status: ReportStatus;
  message: string;
}

export interface ReportStatusResponse {
  trackingCode: string;
  status: ReportStatus;
  category?: string;
  riskLevel?: RiskLevel;
  riskScore?: number;
  createdAt?: string;
  updatedAt?: string;
}


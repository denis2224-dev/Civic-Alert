import { ClaimStatus, RiskLevel } from './claim-check.model';
import { ReportStatus } from './report.model';

export interface ValidatorReport {
  id: number;
  trackingCode: string;
  claimText: string;
  normalizedText: string;
  platform: string;
  sourceUrl?: string;
  region: string;
  language: string;
  status: ReportStatus;
  category?: string;
  riskLevel?: RiskLevel;
  riskScore?: number;
  matchedPhrase?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ValidatorDetectionLog {
  id: number;
  matchedPhrase?: string;
  category?: string;
  severity?: number;
  riskScore?: number;
  riskLevel?: RiskLevel;
  engineOutput?: string;
  createdAt: string;
}

export interface ValidatorReportDetails {
  report: ValidatorReport;
  detectionLogs: ValidatorDetectionLog[];
}

export interface ValidatorDecisionRequest {
  status: ClaimStatus;
  correctionText?: string;
  officialSource?: string;
  officialSourceUrl?: string;
  publish: boolean;
}


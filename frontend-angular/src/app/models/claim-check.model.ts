export type ClaimStatus =
  | 'VERIFIED_TRUE'
  | 'VERIFIED_FALSE'
  | 'MISLEADING'
  | 'NEEDS_CONTEXT'
  | 'NEEDS_REVIEW'
  | 'NO_MATCH_FOUND'
  | 'REJECTED';

export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface ClaimCheckRequest {
  text: string;
  region: string;
  language: string;
}

export interface ClaimCheckResponse {
  status: ClaimStatus;
  riskLevel: RiskLevel;
  category: string;
  message: string;
  correction?: string;
  officialSource?: string;
  officialSourceUrl?: string;
  lastUpdated?: string;
}


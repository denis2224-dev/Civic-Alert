import { ClaimStatus } from './claim-check.model';

export interface VerifiedClaim {
  id: number;
  claimText: string;
  category: string;
  status: ClaimStatus;
  correctionText?: string;
  officialSource?: string;
  officialSourceUrl?: string;
  language: string;
  region: string;
  updatedAt: string;
}


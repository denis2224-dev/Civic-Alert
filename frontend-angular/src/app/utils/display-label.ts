const LABEL_OVERRIDES: Record<string, string> = {
  verified_true: 'Verified True',
  verified_false: 'Verified False',
  misleading: 'Misleading',
  needs_review: 'Needs Review',
  no_match_found: 'No Verified Match Found',
  needs_context: 'Needs Context',
  received: 'Received',
  resolved: 'Resolved',
  published: 'Published',
  merged: 'Merged',
  rejected: 'Rejected',
  under_review: 'Under Review',
  fake_voting_method: 'Fake Voting Method',
  voter_suppression: 'Voter Suppression',
  voting_process: 'Voting Process',
  voting_date: 'Voting Date',
  voting_hours: 'Voting Hours',
  polling_location: 'Polling Location',
  ballot_confusion: 'Ballot Confusion',
  id_requirement: 'ID Requirement',
  results_misinformation: 'Results Misinformation',
  official_impersonation: 'Official Impersonation',
  observer_misinformation: 'Observer Misinformation',
  diaspora_voting: 'Diaspora Voting'
};

export function toDisplayLabel(value?: string | null): string {
  if (!value || !value.trim()) {
    return 'N/A';
  }

  const normalized = value
    .trim()
    .toLowerCase()
    .replace(/-/g, '_')
    .replace(/\s+/g, '_');

  if (LABEL_OVERRIDES[normalized]) {
    return LABEL_OVERRIDES[normalized];
  }

  return normalized
    .split('_')
    .filter(Boolean)
    .map((token) => token.charAt(0).toUpperCase() + token.slice(1))
    .join(' ');
}

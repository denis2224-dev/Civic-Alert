#ifndef RISK_SCORE_H
#define RISK_SCORE_H

int calculate_risk_score(int severity, int has_matched_phrase, const char *category);
const char *risk_level_from_score(int score);

#endif


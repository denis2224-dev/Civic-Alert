#include "risk_score.h"

#include <string.h>

int calculate_risk_score(int severity, int has_matched_phrase, const char *category) {
    int score = severity * 15;

    if (has_matched_phrase) {
        score += 10;
    }

    if (category != NULL
        && (strcmp(category, "voter_suppression") == 0
            || strcmp(category, "fake_voting_method") == 0)) {
        score += 10;
    }

    return score;
}

const char *risk_level_from_score(int score) {
    if (score >= 80) {
        return "CRITICAL";
    }
    if (score >= 55) {
        return "HIGH";
    }
    if (score >= 30) {
        return "MEDIUM";
    }
    return "LOW";
}


#include "risk_score.h"

#include <string.h>

int calculate_risk_score(int severity, int exact_match, int fuzzy_match, const char *category) {
    int score = severity * 15;

    if (exact_match) {
        score += 10;
    } else if (fuzzy_match) {
        score += 8;
    }

    if (category != NULL
        && (strcmp(category, "voter_suppression") == 0
            || strcmp(category, "fake_voting_method") == 0)) {
        score += 10;
    } else if (category != NULL
               && (strcmp(category, "voting_date") == 0
                   || strcmp(category, "polling_location") == 0)) {
        score += 5;
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

#include "rumor_matcher.h"

#include "normalizer.h"
#include "priority_queue.h"
#include "risk_score.h"

#include <stddef.h>
#include <stdlib.h>
#include <string.h>

static const RumorPattern RUMOR_PATTERNS[] = {
    {"voting cancelled", "voting_process", 5},
    {"election postponed", "voting_date", 5},
    {"polling station moved", "polling_location", 4},
    {"vote by sms", "fake_voting_method", 5},
    {"vote online", "fake_voting_method", 5},
    {"ballots invalid", "ballot_confusion", 4},
    {"do not vote today", "voter_suppression", 5},
    {"voting date changed", "voting_date", 5},
    {"polling stations are closed", "polling_location", 5},
    {"election was cancelled", "voting_process", 5},
    {"election cancelled", "voting_process", 5}
};

static size_t rumor_pattern_count(void) {
    return sizeof(RUMOR_PATTERNS) / sizeof(RUMOR_PATTERNS[0]);
}

void rumor_matcher_load_patterns(TrieNode *root) {
    if (root == NULL) {
        return;
    }

    size_t count = rumor_pattern_count();
    for (size_t i = 0; i < count; i++) {
        char *normalized = normalize_text(RUMOR_PATTERNS[i].phrase);
        if (normalized == NULL) {
            continue;
        }
        trie_insert(root, normalized, (int)i);
        free(normalized);
    }
}

void rumor_matcher_detect(TrieNode *root, const char *normalized_claim, EngineDetectionResult *result) {
    if (result == NULL) {
        return;
    }

    result->matched = 0;
    result->pattern.phrase = NULL;
    result->pattern.category = NULL;
    result->pattern.severity = 0;
    result->risk_score = 0;
    result->risk_level = risk_level_from_score(0);

    if (root == NULL || normalized_claim == NULL || normalized_claim[0] == '\0') {
        return;
    }

    PriorityQueue queue;
    pq_init(&queue, 16);

    size_t text_len = strlen(normalized_claim);
    for (size_t i = 0; i < text_len; i++) {
        TrieNode *node = root;
        for (size_t j = i; j < text_len; j++) {
            unsigned char ch = (unsigned char)normalized_claim[j];
            if (ch >= TRIE_CHARSET) {
                break;
            }

            node = node->children[ch];
            if (node == NULL) {
                break;
            }

            if (node->is_end && node->pattern_index >= 0) {
                const RumorPattern *pattern = &RUMOR_PATTERNS[node->pattern_index];
                int score = calculate_risk_score(pattern->severity, 1, pattern->category);
                pq_push(&queue, (PQItem){.score = score, .pattern_index = node->pattern_index});
            }
        }
    }

    if (!pq_is_empty(&queue)) {
        PQItem best = pq_pop(&queue);
        if (best.pattern_index >= 0 && (size_t)best.pattern_index < rumor_pattern_count()) {
            result->matched = 1;
            result->pattern = RUMOR_PATTERNS[best.pattern_index];
            result->risk_score = best.score;
            result->risk_level = risk_level_from_score(best.score);
        }
    }

    pq_free(&queue);
}


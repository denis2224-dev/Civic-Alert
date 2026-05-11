#ifndef RUMOR_MATCHER_H
#define RUMOR_MATCHER_H

#include "trie.h"

typedef struct {
    const char *phrase;
    const char *category;
    int severity;
} RumorPattern;

typedef struct {
    int matched;
    RumorPattern pattern;
    int risk_score;
    const char *risk_level;
} EngineDetectionResult;

void rumor_matcher_load_patterns(TrieNode *root);
void rumor_matcher_detect(TrieNode *root, const char *normalized_claim, EngineDetectionResult *result);

#endif


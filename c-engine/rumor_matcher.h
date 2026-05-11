#ifndef RUMOR_MATCHER_H
#define RUMOR_MATCHER_H

typedef struct {
    char *phrase;
    char *normalized_phrase;
    char *category;
    int severity;
    char *language;
} RumorPattern;

typedef struct {
    int matched;
    RumorPattern pattern;
    int risk_score;
    const char *risk_level;
} EngineDetectionResult;

typedef struct RumorMatcher RumorMatcher;

RumorMatcher *rumor_matcher_create(void);
int rumor_matcher_init(RumorMatcher *matcher, const char *patterns_file_path, const char *language);
void rumor_matcher_detect(RumorMatcher *matcher, const char *normalized_claim, EngineDetectionResult *result);
void rumor_matcher_destroy(RumorMatcher *matcher);

#endif

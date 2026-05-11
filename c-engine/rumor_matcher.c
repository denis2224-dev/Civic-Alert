#include "rumor_matcher.h"

#include "aho_corasick.h"
#include "edit_distance.h"
#include "hash_table.h"
#include "normalizer.h"
#include "priority_queue.h"
#include "risk_score.h"
#include "trie.h"

#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef struct {
    const char *phrase;
    const char *category;
    int severity;
    const char *language;
} DefaultPattern;

static const DefaultPattern DEFAULT_PATTERNS[] = {
    {"vote by sms", "fake_voting_method", 5, "en"},
    {"voting by sms", "fake_voting_method", 5, "en"},
    {"can i vote by sms", "fake_voting_method", 5, "en"},
    {"election cancelled", "voting_process", 5, "en"},
    {"the election was cancelled", "voting_process", 5, "en"},
    {"polling station moved", "polling_location", 4, "en"},
    {"voting moved to tomorrow", "voting_date", 5, "en"},
    {"do not vote today", "voter_suppression", 5, "en"},
    {"poți vota prin sms", "fake_voting_method", 5, "ro"},
    {"pot sa votez prin sms", "fake_voting_method", 5, "ro"},
    {"alegerile au fost anulate", "voting_process", 5, "ro"},
    {"secția de votare s-a mutat", "polling_location", 4, "ro"},
    {"можно голосовать по смс", "fake_voting_method", 5, "ru"},
    {"голосование по смс", "fake_voting_method", 5, "ru"},
    {"выборы отменены", "voting_process", 5, "ru"},
    {"избирательные участки закрыты", "polling_location", 5, "ru"}
};

struct RumorMatcher {
    RumorPattern *patterns;
    size_t pattern_count;
    size_t pattern_capacity;
    TrieNode *trie_root;
    AhoAutomaton *automaton;
};

typedef struct {
    RumorMatcher *matcher;
    int *hit_counts;
    HashTable *category_counts;
} MatchCollector;

static char *string_duplicate(const char *value) {
    if (value == NULL) {
        return NULL;
    }
    size_t len = strlen(value);
    char *copy = (char *)malloc(len + 1);
    if (copy == NULL) {
        return NULL;
    }
    memcpy(copy, value, len + 1);
    return copy;
}

static char *trim_in_place(char *value) {
    if (value == NULL) {
        return NULL;
    }

    while (*value != '\0' && isspace((unsigned char)*value)) {
        value++;
    }
    if (*value == '\0') {
        return value;
    }

    char *end = value + strlen(value) - 1;
    while (end > value && isspace((unsigned char)*end)) {
        *end = '\0';
        end--;
    }
    return value;
}

static int ensure_pattern_capacity(RumorMatcher *matcher) {
    if (matcher->pattern_count < matcher->pattern_capacity) {
        return 1;
    }

    size_t new_capacity = matcher->pattern_capacity == 0 ? 32 : matcher->pattern_capacity * 2;
    RumorPattern *resized = (RumorPattern *)realloc(matcher->patterns, new_capacity * sizeof(RumorPattern));
    if (resized == NULL) {
        return 0;
    }

    matcher->patterns = resized;
    matcher->pattern_capacity = new_capacity;
    return 1;
}

static void free_pattern(RumorPattern *pattern) {
    if (pattern == NULL) {
        return;
    }
    free(pattern->phrase);
    free(pattern->normalized_phrase);
    free(pattern->category);
    free(pattern->language);
    pattern->phrase = NULL;
    pattern->normalized_phrase = NULL;
    pattern->category = NULL;
    pattern->language = NULL;
    pattern->severity = 0;
}

static int append_pattern(RumorMatcher *matcher, const char *phrase, const char *category, int severity, const char *language) {
    if (matcher == NULL || phrase == NULL || phrase[0] == '\0' || category == NULL || category[0] == '\0') {
        return 0;
    }

    char *normalized_phrase = normalize_text(phrase);
    if (normalized_phrase == NULL || normalized_phrase[0] == '\0') {
        free(normalized_phrase);
        return 0;
    }

    if (!ensure_pattern_capacity(matcher)) {
        free(normalized_phrase);
        return 0;
    }

    RumorPattern pattern = {0};
    pattern.phrase = string_duplicate(phrase);
    pattern.normalized_phrase = normalized_phrase;
    pattern.category = string_duplicate(category);
    pattern.language = string_duplicate(language == NULL ? "" : language);
    pattern.severity = severity < 1 ? 1 : severity;

    if (pattern.phrase == NULL || pattern.category == NULL || pattern.language == NULL) {
        free_pattern(&pattern);
        return 0;
    }

    size_t pattern_index = matcher->pattern_count;
    matcher->patterns[matcher->pattern_count++] = pattern;

    trie_insert(matcher->trie_root, pattern.normalized_phrase, (int)pattern_index);
    if (!aho_add_pattern(matcher->automaton, pattern.normalized_phrase, (int)pattern_index)) {
        matcher->pattern_count--;
        free_pattern(&matcher->patterns[matcher->pattern_count]);
        return 0;
    }
    return 1;
}

static int should_use_pattern_language(const char *pattern_language, const char *requested_language) {
    if (requested_language == NULL || requested_language[0] == '\0') {
        return 1;
    }
    if (pattern_language == NULL || pattern_language[0] == '\0') {
        return 1;
    }
    return strcmp(pattern_language, requested_language) == 0;
}

static int load_default_patterns(RumorMatcher *matcher, const char *language) {
    size_t count = sizeof(DEFAULT_PATTERNS) / sizeof(DEFAULT_PATTERNS[0]);
    int inserted = 0;
    for (size_t i = 0; i < count; i++) {
        if (!should_use_pattern_language(DEFAULT_PATTERNS[i].language, language)) {
            continue;
        }
        if (append_pattern(
                matcher,
                DEFAULT_PATTERNS[i].phrase,
                DEFAULT_PATTERNS[i].category,
                DEFAULT_PATTERNS[i].severity,
                DEFAULT_PATTERNS[i].language)) {
            inserted++;
        }
    }
    return inserted;
}

static int parse_pattern_line(
        RumorMatcher *matcher,
        char *line,
        const char *language_filter
) {
    char *phrase = strtok(line, "|");
    char *category = strtok(NULL, "|");
    char *severity_text = strtok(NULL, "|");
    char *language = strtok(NULL, "|");

    if (phrase == NULL || category == NULL || severity_text == NULL || language == NULL) {
        return 0;
    }

    phrase = trim_in_place(phrase);
    category = trim_in_place(category);
    severity_text = trim_in_place(severity_text);
    language = trim_in_place(language);

    if (phrase[0] == '\0' || category[0] == '\0' || severity_text[0] == '\0') {
        return 0;
    }

    if (!should_use_pattern_language(language, language_filter)) {
        return 0;
    }

    int severity = atoi(severity_text);
    return append_pattern(matcher, phrase, category, severity, language);
}

static int load_patterns_from_file(RumorMatcher *matcher, const char *patterns_file_path, const char *language) {
    if (patterns_file_path == NULL || patterns_file_path[0] == '\0') {
        return 0;
    }

    FILE *file = fopen(patterns_file_path, "r");
    if (file == NULL) {
        return 0;
    }

    char buffer[4096];
    int inserted = 0;
    while (fgets(buffer, sizeof(buffer), file) != NULL) {
        char *line = trim_in_place(buffer);
        if (line[0] == '\0' || line[0] == '#') {
            continue;
        }
        if (strncmp(line, "## ", 3) == 0) {
            continue;
        }

        char copy[4096];
        size_t len = strlen(line);
        if (len >= sizeof(copy)) {
            continue;
        }
        memcpy(copy, line, len + 1);
        inserted += parse_pattern_line(matcher, copy, language);
    }

    fclose(file);
    return inserted;
}

static void collect_match(int pattern_index, void *user_data) {
    MatchCollector *collector = (MatchCollector *)user_data;
    if (collector == NULL
            || collector->matcher == NULL
            || collector->hit_counts == NULL
            || collector->category_counts == NULL) {
        return;
    }

    if (pattern_index < 0 || (size_t)pattern_index >= collector->matcher->pattern_count) {
        return;
    }

    collector->hit_counts[pattern_index]++;
    hash_table_increment(collector->category_counts, collector->matcher->patterns[pattern_index].category);
}

static void initialize_result(EngineDetectionResult *result) {
    if (result == NULL) {
        return;
    }
    result->matched = 0;
    result->pattern.phrase = NULL;
    result->pattern.normalized_phrase = NULL;
    result->pattern.category = NULL;
    result->pattern.language = NULL;
    result->pattern.severity = 0;
    result->risk_score = 0;
    result->risk_level = "LOW";
}

static void rumor_matcher_release(RumorMatcher *matcher) {
    if (matcher == NULL) {
        return;
    }

    if (matcher->patterns != NULL) {
        for (size_t i = 0; i < matcher->pattern_count; i++) {
            free_pattern(&matcher->patterns[i]);
        }
        free(matcher->patterns);
        matcher->patterns = NULL;
    }
    matcher->pattern_count = 0;
    matcher->pattern_capacity = 0;

    trie_free(matcher->trie_root);
    matcher->trie_root = NULL;

    aho_free(matcher->automaton);
    matcher->automaton = NULL;
}

RumorMatcher *rumor_matcher_create(void) {
    return (RumorMatcher *)calloc(1, sizeof(RumorMatcher));
}

int rumor_matcher_init(RumorMatcher *matcher, const char *patterns_file_path, const char *language) {
    if (matcher == NULL) {
        return 0;
    }

    matcher->patterns = NULL;
    matcher->pattern_count = 0;
    matcher->pattern_capacity = 0;
    matcher->trie_root = trie_create_node();
    matcher->automaton = aho_create();

    if (matcher->trie_root == NULL || matcher->automaton == NULL) {
        rumor_matcher_release(matcher);
        return 0;
    }

    int loaded = load_patterns_from_file(matcher, patterns_file_path, language);
    if (loaded == 0) {
        loaded = load_default_patterns(matcher, language);
    }
    if (loaded == 0) {
        rumor_matcher_release(matcher);
        return 0;
    }

    if (!aho_build(matcher->automaton)) {
        rumor_matcher_release(matcher);
        return 0;
    }

    return 1;
}

void rumor_matcher_detect(RumorMatcher *matcher, const char *normalized_claim, EngineDetectionResult *result) {
    initialize_result(result);
    if (matcher == NULL || normalized_claim == NULL || normalized_claim[0] == '\0' || matcher->pattern_count == 0) {
        return;
    }

    PriorityQueue queue;
    pq_init(&queue, 16);
    if (queue.items == NULL) {
        return;
    }

    int *hit_counts = (int *)calloc(matcher->pattern_count, sizeof(int));
    HashTable *category_counts = hash_table_create(128);
    if (hit_counts == NULL || category_counts == NULL) {
        free(hit_counts);
        hash_table_destroy(category_counts);
        pq_free(&queue);
        return;
    }

    MatchCollector collector = {
            .matcher = matcher,
            .hit_counts = hit_counts,
            .category_counts = category_counts
    };

    aho_search(matcher->automaton, normalized_claim, collect_match, &collector);

    for (size_t i = 0; i < matcher->pattern_count; i++) {
        if (hit_counts[i] <= 0) {
            continue;
        }

        RumorPattern *pattern = &matcher->patterns[i];
        int category_count = hash_table_get(category_counts, pattern->category);
        int score = calculate_risk_score(pattern->severity, 1, 0, pattern->category);
        if (hit_counts[i] > 1) {
            score += (hit_counts[i] - 1) * 2;
        }
        if (category_count > 1) {
            score += category_count - 1;
        }

        pq_push(&queue, (PQItem){
                .score = score,
                .pattern_index = (int)i,
                .exact_match = 1,
                .fuzzy_match = 0
        });
    }

    if (pq_is_empty(&queue)) {
        int trie_hint = trie_search_in_text(matcher->trie_root, normalized_claim);
        double threshold = trie_hint ? 0.68 : 0.74;
        size_t claim_len = strlen(normalized_claim);

        for (size_t i = 0; i < matcher->pattern_count; i++) {
            RumorPattern *pattern = &matcher->patterns[i];
            if (pattern->normalized_phrase == NULL || pattern->normalized_phrase[0] == '\0') {
                continue;
            }

            size_t phrase_len = strlen(pattern->normalized_phrase);
            size_t max_len = claim_len > phrase_len ? claim_len : phrase_len;
            size_t min_len = claim_len < phrase_len ? claim_len : phrase_len;
            if (max_len > min_len * 2 + 6) {
                continue;
            }

            double similarity = levenshtein_similarity(normalized_claim, pattern->normalized_phrase);
            if (similarity < threshold) {
                continue;
            }

            hash_table_increment(category_counts, pattern->category);
            int score = calculate_risk_score(pattern->severity, 0, 1, pattern->category);
            pq_push(&queue, (PQItem){
                    .score = score,
                    .pattern_index = (int)i,
                    .exact_match = 0,
                    .fuzzy_match = 1
            });
        }
    }

    if (!pq_is_empty(&queue)) {
        PQItem best = pq_pop(&queue);
        if (best.pattern_index >= 0 && (size_t)best.pattern_index < matcher->pattern_count) {
            RumorPattern *pattern = &matcher->patterns[best.pattern_index];
            result->matched = 1;
            result->pattern = *pattern;
            result->risk_score = best.score;
            result->risk_level = risk_level_from_score(best.score);
        }
    }

    free(hit_counts);
    hash_table_destroy(category_counts);
    pq_free(&queue);
}

void rumor_matcher_destroy(RumorMatcher *matcher) {
    if (matcher == NULL) {
        return;
    }
    rumor_matcher_release(matcher);
    free(matcher);
}

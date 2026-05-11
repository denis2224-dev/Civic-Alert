#include "json_writer.h"
#include "normalizer.h"
#include "rumor_matcher.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef struct {
    char *claim;
    const char *patterns_file;
    const char *language;
} EngineArgs;

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

static char *join_args(int start_index, int argc, char **argv) {
    if (argc <= start_index) {
        char *empty = (char *)malloc(1);
        if (empty != NULL) {
            empty[0] = '\0';
        }
        return empty;
    }

    size_t total = 0;
    for (int i = start_index; i < argc; i++) {
        total += strlen(argv[i]) + 1;
    }

    char *buffer = (char *)malloc(total + 1);
    if (buffer == NULL) {
        return NULL;
    }

    size_t offset = 0;
    for (int i = start_index; i < argc; i++) {
        size_t part_len = strlen(argv[i]);
        memcpy(buffer + offset, argv[i], part_len);
        offset += part_len;
        if (i < argc - 1) {
            buffer[offset++] = ' ';
        }
    }
    buffer[offset] = '\0';
    return buffer;
}

static int parse_args(int argc, char **argv, EngineArgs *parsed) {
    if (parsed == NULL) {
        return 0;
    }

    parsed->claim = NULL;
    parsed->patterns_file = NULL;
    parsed->language = NULL;

    int has_flag = 0;
    for (int i = 1; i < argc; i++) {
        if (strncmp(argv[i], "--", 2) == 0) {
            has_flag = 1;
            break;
        }
    }

    if (!has_flag) {
        parsed->claim = join_args(1, argc, argv);
        return parsed->claim != NULL;
    }

    for (int i = 1; i < argc; i++) {
        if (strcmp(argv[i], "--claim") == 0 && i + 1 < argc) {
            free(parsed->claim);
            parsed->claim = string_duplicate(argv[++i]);
        } else if (strcmp(argv[i], "--patterns") == 0 && i + 1 < argc) {
            parsed->patterns_file = argv[++i];
        } else if (strcmp(argv[i], "--language") == 0 && i + 1 < argc) {
            parsed->language = argv[++i];
        }
    }

    if (parsed->claim == NULL) {
        parsed->claim = join_args(1, argc, argv);
    }
    return parsed->claim != NULL;
}

static void free_engine_args(EngineArgs *parsed) {
    if (parsed == NULL) {
        return;
    }
    free(parsed->claim);
    parsed->claim = NULL;
}

int main(int argc, char **argv) {
    EngineArgs args;
    if (!parse_args(argc, argv, &args)) {
        fprintf(stderr, "Memory allocation failed.\n");
        return 1;
    }

    char *normalized = normalize_text(args.claim);
    if (normalized == NULL) {
        free_engine_args(&args);
        fprintf(stderr, "Normalization failed.\n");
        return 1;
    }

    RumorMatcher *matcher = rumor_matcher_create();
    if (matcher == NULL || !rumor_matcher_init(matcher, args.patterns_file, args.language)) {
        free(normalized);
        free_engine_args(&args);
        rumor_matcher_destroy(matcher);
        fprintf(stderr, "Pattern matcher initialization failed.\n");
        return 1;
    }

    EngineDetectionResult result;
    rumor_matcher_detect(matcher, normalized, &result);
    json_writer_print(&result);

    rumor_matcher_destroy(matcher);
    free(normalized);
    free_engine_args(&args);
    return 0;
}

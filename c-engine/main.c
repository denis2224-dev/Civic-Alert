#include "json_writer.h"
#include "normalizer.h"
#include "rumor_matcher.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static char *join_args(int argc, char **argv) {
    if (argc <= 1) {
        char *empty = (char *)malloc(1);
        if (empty != NULL) {
            empty[0] = '\0';
        }
        return empty;
    }

    size_t total = 0;
    for (int i = 1; i < argc; i++) {
        total += strlen(argv[i]) + 1;
    }

    char *buffer = (char *)malloc(total + 1);
    if (buffer == NULL) {
        return NULL;
    }

    size_t offset = 0;
    for (int i = 1; i < argc; i++) {
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

int main(int argc, char **argv) {
    char *raw_text = join_args(argc, argv);
    if (raw_text == NULL) {
        fprintf(stderr, "Memory allocation failed.\n");
        return 1;
    }

    char *normalized = normalize_text(raw_text);
    free(raw_text);
    if (normalized == NULL) {
        fprintf(stderr, "Normalization failed.\n");
        return 1;
    }

    RumorMatcher *matcher = rumor_matcher_create();
    if (matcher == NULL || !rumor_matcher_init(matcher, NULL, NULL)) {
        free(normalized);
        rumor_matcher_destroy(matcher);
        fprintf(stderr, "Pattern matcher initialization failed.\n");
        return 1;
    }

    EngineDetectionResult result;
    rumor_matcher_detect(matcher, normalized, &result);
    json_writer_print(&result);

    rumor_matcher_destroy(matcher);
    free(normalized);
    return 0;
}

#include "normalizer.h"

#include <ctype.h>
#include <stdlib.h>
#include <string.h>

char *normalize_text(const char *input) {
    if (input == NULL) {
        char *empty = (char *)malloc(1);
        if (empty != NULL) {
            empty[0] = '\0';
        }
        return empty;
    }

    size_t len = strlen(input);
    char *normalized = (char *)malloc(len + 1);
    if (normalized == NULL) {
        return NULL;
    }

    size_t out = 0;
    int previous_was_space = 1;

    for (size_t i = 0; i < len; i++) {
        unsigned char ch = (unsigned char)input[i];

        if (ch >= 128) {
            normalized[out++] = (char)ch;
            previous_was_space = 0;
            continue;
        }

        if (isalnum(ch)) {
            normalized[out++] = (char)tolower(ch);
            previous_was_space = 0;
            continue;
        }

        if ((isspace(ch) || ispunct(ch)) && !previous_was_space) {
            normalized[out++] = ' ';
            previous_was_space = 1;
        }
    }

    if (out > 0 && normalized[out - 1] == ' ') {
        out--;
    }

    normalized[out] = '\0';
    return normalized;
}

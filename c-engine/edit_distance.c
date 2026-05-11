#include "edit_distance.h"

#include <stdlib.h>
#include <string.h>

static int min3(int a, int b, int c) {
    int min = a < b ? a : b;
    return min < c ? min : c;
}

int levenshtein_distance(const char *left, const char *right) {
    if (left == NULL || right == NULL) {
        return 0;
    }

    size_t left_len = strlen(left);
    size_t right_len = strlen(right);

    if (left_len == 0) {
        return (int)right_len;
    }
    if (right_len == 0) {
        return (int)left_len;
    }

    int *previous = (int *)malloc((right_len + 1) * sizeof(int));
    int *current = (int *)malloc((right_len + 1) * sizeof(int));
    if (previous == NULL || current == NULL) {
        free(previous);
        free(current);
        return (int)(left_len > right_len ? left_len : right_len);
    }

    for (size_t j = 0; j <= right_len; j++) {
        previous[j] = (int)j;
    }

    for (size_t i = 1; i <= left_len; i++) {
        current[0] = (int)i;
        for (size_t j = 1; j <= right_len; j++) {
            int substitution_cost = left[i - 1] == right[j - 1] ? 0 : 1;
            current[j] = min3(
                    previous[j] + 1,
                    current[j - 1] + 1,
                    previous[j - 1] + substitution_cost
            );
        }

        int *tmp = previous;
        previous = current;
        current = tmp;
    }

    int distance = previous[right_len];
    free(previous);
    free(current);
    return distance;
}

double levenshtein_similarity(const char *left, const char *right) {
    if (left == NULL || right == NULL) {
        return 0.0;
    }

    size_t left_len = strlen(left);
    size_t right_len = strlen(right);
    size_t max_len = left_len > right_len ? left_len : right_len;
    if (max_len == 0) {
        return 1.0;
    }

    int distance = levenshtein_distance(left, right);
    if (distance < 0) {
        return 0.0;
    }

    double similarity = 1.0 - ((double)distance / (double)max_len);
    if (similarity < 0.0) {
        return 0.0;
    }
    if (similarity > 1.0) {
        return 1.0;
    }
    return similarity;
}

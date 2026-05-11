#include "aho_corasick.h"

#include <stddef.h>
#include <stdlib.h>

#define AC_CHARSET 256

typedef struct AhoNode {
    struct AhoNode *children[AC_CHARSET];
    struct AhoNode *fail;
    int *output;
    size_t output_count;
    size_t output_capacity;
} AhoNode;

struct AhoAutomaton {
    AhoNode *root;
};

static AhoNode *aho_node_create(void) {
    AhoNode *node = (AhoNode *)calloc(1, sizeof(AhoNode));
    return node;
}

static int aho_node_add_output(AhoNode *node, int pattern_index) {
    if (node == NULL) {
        return 0;
    }

    if (node->output_count == node->output_capacity) {
        size_t new_capacity = node->output_capacity == 0 ? 2 : node->output_capacity * 2;
        int *resized = (int *)realloc(node->output, new_capacity * sizeof(int));
        if (resized == NULL) {
            return 0;
        }
        node->output = resized;
        node->output_capacity = new_capacity;
    }

    node->output[node->output_count++] = pattern_index;
    return 1;
}

static int aho_node_append_outputs(AhoNode *target, const AhoNode *source) {
    if (target == NULL || source == NULL || source->output_count == 0) {
        return 1;
    }

    for (size_t i = 0; i < source->output_count; i++) {
        if (!aho_node_add_output(target, source->output[i])) {
            return 0;
        }
    }
    return 1;
}

static void aho_node_free(AhoNode *node) {
    if (node == NULL) {
        return;
    }
    for (int i = 0; i < AC_CHARSET; i++) {
        aho_node_free(node->children[i]);
    }
    free(node->output);
    free(node);
}

AhoAutomaton *aho_create(void) {
    AhoAutomaton *automaton = (AhoAutomaton *)calloc(1, sizeof(AhoAutomaton));
    if (automaton == NULL) {
        return NULL;
    }

    automaton->root = aho_node_create();
    if (automaton->root == NULL) {
        free(automaton);
        return NULL;
    }
    automaton->root->fail = automaton->root;
    return automaton;
}

int aho_add_pattern(AhoAutomaton *automaton, const char *pattern, int pattern_index) {
    if (automaton == NULL || automaton->root == NULL || pattern == NULL || pattern[0] == '\0') {
        return 0;
    }

    AhoNode *node = automaton->root;
    for (const unsigned char *p = (const unsigned char *)pattern; *p != '\0'; p++) {
        unsigned char ch = *p;
        if (node->children[ch] == NULL) {
            node->children[ch] = aho_node_create();
            if (node->children[ch] == NULL) {
                return 0;
            }
        }
        node = node->children[ch];
    }

    return aho_node_add_output(node, pattern_index);
}

int aho_build(AhoAutomaton *automaton) {
    if (automaton == NULL || automaton->root == NULL) {
        return 0;
    }

    size_t queue_capacity = 128;
    AhoNode **queue = (AhoNode **)malloc(queue_capacity * sizeof(AhoNode *));
    if (queue == NULL) {
        return 0;
    }
    size_t head = 0;
    size_t tail = 0;

    AhoNode *root = automaton->root;
    for (int i = 0; i < AC_CHARSET; i++) {
        AhoNode *child = root->children[i];
        if (child != NULL) {
            child->fail = root;
            if (tail == queue_capacity) {
                size_t new_capacity = queue_capacity * 2;
                AhoNode **resized = (AhoNode **)realloc(queue, new_capacity * sizeof(AhoNode *));
                if (resized == NULL) {
                    free(queue);
                    return 0;
                }
                queue = resized;
                queue_capacity = new_capacity;
            }
            queue[tail++] = child;
        }
    }

    while (head < tail) {
        AhoNode *current = queue[head++];

        for (int i = 0; i < AC_CHARSET; i++) {
            AhoNode *child = current->children[i];
            if (child == NULL) {
                continue;
            }

            AhoNode *fail = current->fail;
            while (fail != root && fail->children[i] == NULL) {
                fail = fail->fail;
            }
            if (fail->children[i] != NULL && fail->children[i] != child) {
                fail = fail->children[i];
            }

            child->fail = fail;
            if (!aho_node_append_outputs(child, fail)) {
                free(queue);
                return 0;
            }

            if (tail == queue_capacity) {
                size_t new_capacity = queue_capacity * 2;
                AhoNode **resized = (AhoNode **)realloc(queue, new_capacity * sizeof(AhoNode *));
                if (resized == NULL) {
                    free(queue);
                    return 0;
                }
                queue = resized;
                queue_capacity = new_capacity;
            }
            queue[tail++] = child;
        }
    }

    free(queue);
    return 1;
}

void aho_search(const AhoAutomaton *automaton, const char *text, AhoMatchCallback callback, void *user_data) {
    if (automaton == NULL || automaton->root == NULL || text == NULL || callback == NULL) {
        return;
    }

    const AhoNode *root = automaton->root;
    const AhoNode *node = root;

    for (const unsigned char *p = (const unsigned char *)text; *p != '\0'; p++) {
        unsigned char ch = *p;

        while (node != root && node->children[ch] == NULL) {
            node = node->fail;
        }

        if (node->children[ch] != NULL) {
            node = node->children[ch];
        }

        for (size_t i = 0; i < node->output_count; i++) {
            callback(node->output[i], user_data);
        }
    }
}

void aho_free(AhoAutomaton *automaton) {
    if (automaton == NULL) {
        return;
    }
    aho_node_free(automaton->root);
    free(automaton);
}

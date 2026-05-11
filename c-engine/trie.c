#include "trie.h"

#include <stddef.h>
#include <stdlib.h>
#include <string.h>

TrieNode *trie_create_node(void) {
    TrieNode *node = (TrieNode *)calloc(1, sizeof(TrieNode));
    if (node != NULL) {
        node->is_end = 0;
        node->pattern_index = -1;
    }
    return node;
}

void trie_insert(TrieNode *root, const char *phrase, int pattern_index) {
    if (root == NULL || phrase == NULL) {
        return;
    }

    TrieNode *current = root;
    for (size_t i = 0; phrase[i] != '\0'; i++) {
        unsigned char ch = (unsigned char)phrase[i];
        if (current->children[ch] == NULL) {
            current->children[ch] = trie_create_node();
            if (current->children[ch] == NULL) {
                return;
            }
        }
        current = current->children[ch];
    }

    current->is_end = 1;
    current->pattern_index = pattern_index;
}

int trie_search_in_text(const TrieNode *root, const char *text) {
    if (root == NULL || text == NULL || text[0] == '\0') {
        return 0;
    }

    size_t text_len = strlen(text);
    for (size_t i = 0; i < text_len; i++) {
        const TrieNode *node = root;
        for (size_t j = i; j < text_len; j++) {
            unsigned char ch = (unsigned char)text[j];
            node = node->children[ch];
            if (node == NULL) {
                break;
            }
            if (node->is_end) {
                return 1;
            }
        }
    }
    return 0;
}

void trie_free(TrieNode *root) {
    if (root == NULL) {
        return;
    }

    for (int i = 0; i < TRIE_CHARSET; i++) {
        trie_free(root->children[i]);
    }
    free(root);
}

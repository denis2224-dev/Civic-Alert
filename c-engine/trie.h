#ifndef TRIE_H
#define TRIE_H

#define TRIE_CHARSET 128

typedef struct TrieNode {
    struct TrieNode *children[TRIE_CHARSET];
    int is_end;
    int pattern_index;
} TrieNode;

TrieNode *trie_create_node(void);
void trie_insert(TrieNode *root, const char *phrase, int pattern_index);
void trie_free(TrieNode *root);

#endif


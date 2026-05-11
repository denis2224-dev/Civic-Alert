#include "hash_table.h"

#include <stdint.h>
#include <stdlib.h>
#include <string.h>

typedef struct {
    char *key;
    int count;
    int used;
} HashEntry;

struct HashTable {
    HashEntry *entries;
    size_t capacity;
};

static uint64_t hash_key(const char *key) {
    uint64_t hash = 1469598103934665603ULL;
    if (key == NULL) {
        return hash;
    }
    for (const unsigned char *p = (const unsigned char *)key; *p != '\0'; p++) {
        hash ^= (uint64_t)(*p);
        hash *= 1099511628211ULL;
    }
    return hash;
}

static char *copy_string(const char *value) {
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

HashTable *hash_table_create(size_t capacity) {
    size_t safe_capacity = capacity < 16 ? 16 : capacity;
    HashTable *table = (HashTable *)calloc(1, sizeof(HashTable));
    if (table == NULL) {
        return NULL;
    }

    table->entries = (HashEntry *)calloc(safe_capacity, sizeof(HashEntry));
    if (table->entries == NULL) {
        free(table);
        return NULL;
    }

    table->capacity = safe_capacity;
    return table;
}

void hash_table_increment(HashTable *table, const char *key) {
    if (table == NULL || table->entries == NULL || key == NULL || key[0] == '\0') {
        return;
    }

    uint64_t hash = hash_key(key);
    for (size_t i = 0; i < table->capacity; i++) {
        size_t index = (size_t)((hash + i) % table->capacity);
        HashEntry *entry = &table->entries[index];

        if (!entry->used) {
            entry->key = copy_string(key);
            if (entry->key == NULL) {
                return;
            }
            entry->count = 1;
            entry->used = 1;
            return;
        }

        if (entry->key != NULL && strcmp(entry->key, key) == 0) {
            entry->count++;
            return;
        }
    }
}

int hash_table_get(const HashTable *table, const char *key) {
    if (table == NULL || table->entries == NULL || key == NULL || key[0] == '\0') {
        return 0;
    }

    uint64_t hash = hash_key(key);
    for (size_t i = 0; i < table->capacity; i++) {
        size_t index = (size_t)((hash + i) % table->capacity);
        const HashEntry *entry = &table->entries[index];
        if (!entry->used) {
            return 0;
        }
        if (entry->key != NULL && strcmp(entry->key, key) == 0) {
            return entry->count;
        }
    }
    return 0;
}

void hash_table_destroy(HashTable *table) {
    if (table == NULL) {
        return;
    }

    if (table->entries != NULL) {
        for (size_t i = 0; i < table->capacity; i++) {
            free(table->entries[i].key);
            table->entries[i].key = NULL;
        }
        free(table->entries);
        table->entries = NULL;
    }
    free(table);
}

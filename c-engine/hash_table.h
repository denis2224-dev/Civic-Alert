#ifndef HASH_TABLE_H
#define HASH_TABLE_H

#include <stddef.h>

typedef struct HashTable HashTable;

HashTable *hash_table_create(size_t capacity);
void hash_table_increment(HashTable *table, const char *key);
int hash_table_get(const HashTable *table, const char *key);
void hash_table_destroy(HashTable *table);

#endif

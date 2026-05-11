#ifndef PRIORITY_QUEUE_H
#define PRIORITY_QUEUE_H

#include <stddef.h>

typedef struct {
    int score;
    int pattern_index;
} PQItem;

typedef struct {
    PQItem *items;
    size_t size;
    size_t capacity;
} PriorityQueue;

void pq_init(PriorityQueue *queue, size_t initial_capacity);
void pq_push(PriorityQueue *queue, PQItem item);
PQItem pq_pop(PriorityQueue *queue);
int pq_is_empty(const PriorityQueue *queue);
void pq_free(PriorityQueue *queue);

#endif


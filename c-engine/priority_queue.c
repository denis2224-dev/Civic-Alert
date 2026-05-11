#include "priority_queue.h"

#include <stdlib.h>

static void swap(PQItem *a, PQItem *b) {
    PQItem temp = *a;
    *a = *b;
    *b = temp;
}

static void heapify_up(PriorityQueue *queue, size_t index) {
    while (index > 0) {
        size_t parent = (index - 1) / 2;
        if (queue->items[parent].score >= queue->items[index].score) {
            break;
        }
        swap(&queue->items[parent], &queue->items[index]);
        index = parent;
    }
}

static void heapify_down(PriorityQueue *queue, size_t index) {
    while (1) {
        size_t left = index * 2 + 1;
        size_t right = index * 2 + 2;
        size_t largest = index;

        if (left < queue->size && queue->items[left].score > queue->items[largest].score) {
            largest = left;
        }
        if (right < queue->size && queue->items[right].score > queue->items[largest].score) {
            largest = right;
        }
        if (largest == index) {
            break;
        }

        swap(&queue->items[index], &queue->items[largest]);
        index = largest;
    }
}

void pq_init(PriorityQueue *queue, size_t initial_capacity) {
    if (queue == NULL) {
        return;
    }

    size_t capacity = initial_capacity == 0 ? 8 : initial_capacity;
    queue->items = (PQItem *)malloc(capacity * sizeof(PQItem));
    queue->size = 0;
    queue->capacity = queue->items == NULL ? 0 : capacity;
}

void pq_push(PriorityQueue *queue, PQItem item) {
    if (queue == NULL || queue->items == NULL) {
        return;
    }

    if (queue->size == queue->capacity) {
        size_t new_capacity = queue->capacity * 2;
        PQItem *resized = (PQItem *)realloc(queue->items, new_capacity * sizeof(PQItem));
        if (resized == NULL) {
            return;
        }
        queue->items = resized;
        queue->capacity = new_capacity;
    }

    queue->items[queue->size] = item;
    heapify_up(queue, queue->size);
    queue->size++;
}

PQItem pq_pop(PriorityQueue *queue) {
    PQItem empty = {.score = -1, .pattern_index = -1};
    if (queue == NULL || queue->size == 0 || queue->items == NULL) {
        return empty;
    }

    PQItem top = queue->items[0];
    queue->size--;
    if (queue->size > 0) {
        queue->items[0] = queue->items[queue->size];
        heapify_down(queue, 0);
    }
    return top;
}

int pq_is_empty(const PriorityQueue *queue) {
    return queue == NULL || queue->size == 0;
}

void pq_free(PriorityQueue *queue) {
    if (queue == NULL) {
        return;
    }
    free(queue->items);
    queue->items = NULL;
    queue->size = 0;
    queue->capacity = 0;
}


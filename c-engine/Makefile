CC = gcc
CFLAGS = -std=c11 -Wall -Wextra -O2
TARGET = civic_alert_engine

SOURCES = main.c normalizer.c trie.c priority_queue.c rumor_matcher.c risk_score.c json_writer.c
SOURCES += aho_corasick.c edit_distance.c hash_table.c
OBJECTS = $(SOURCES:.c=.o)

all: $(TARGET)

$(TARGET): $(OBJECTS)
	$(CC) $(CFLAGS) -o $(TARGET) $(OBJECTS)

%.o: %.c
	$(CC) $(CFLAGS) -c $< -o $@

clean:
	rm -f $(OBJECTS) $(TARGET)

.PHONY: all clean

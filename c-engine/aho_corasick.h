#ifndef AHO_CORASICK_H
#define AHO_CORASICK_H

typedef struct AhoAutomaton AhoAutomaton;

typedef void (*AhoMatchCallback)(int pattern_index, void *user_data);

AhoAutomaton *aho_create(void);
int aho_add_pattern(AhoAutomaton *automaton, const char *pattern, int pattern_index);
int aho_build(AhoAutomaton *automaton);
void aho_search(const AhoAutomaton *automaton, const char *text, AhoMatchCallback callback, void *user_data);
void aho_free(AhoAutomaton *automaton);

#endif

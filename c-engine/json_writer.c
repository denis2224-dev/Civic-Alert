#include "json_writer.h"

#include <stdio.h>

static void write_json_string(const char *value) {
    if (value == NULL) {
        fputs("null", stdout);
        return;
    }

    fputc('"', stdout);
    for (const char *p = value; *p != '\0'; p++) {
        if (*p == '"' || *p == '\\') {
            fputc('\\', stdout);
        }
        fputc(*p, stdout);
    }
    fputc('"', stdout);
}

void json_writer_print(const EngineDetectionResult *result) {
    if (result == NULL || !result->matched) {
        fputs("{\"matched\":false,\"matchedPhrase\":null,\"category\":null,\"severity\":0,"
              "\"riskScore\":0,\"riskLevel\":\"LOW\"}\n",
              stdout);
        return;
    }

    fputs("{\"matched\":true,\"matchedPhrase\":", stdout);
    write_json_string(result->pattern.phrase);
    fputs(",\"category\":", stdout);
    write_json_string(result->pattern.category);
    fprintf(stdout, ",\"severity\":%d,\"riskScore\":%d,\"riskLevel\":", result->pattern.severity, result->risk_score);
    write_json_string(result->risk_level);
    fputs("}\n", stdout);
}


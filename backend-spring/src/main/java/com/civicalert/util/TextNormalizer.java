package com.civicalert.util;

import java.util.Locale;

public final class TextNormalizer {

    private TextNormalizer() {
    }

    public static String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String lowered = text.toLowerCase(Locale.ROOT);
        String withoutPunctuation = lowered.replaceAll("[^\\p{L}\\p{N}\\s]", " ");
        return withoutPunctuation.replaceAll("\\s+", " ").trim();
    }
}


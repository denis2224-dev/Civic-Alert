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
        String folded = foldRomanianDiacritics(lowered);
        String withoutPunctuation = folded.replaceAll("[^\\p{L}\\p{N}\\s]", " ");
        return withoutPunctuation.replaceAll("\\s+", " ").trim();
    }

    private static String foldRomanianDiacritics(String text) {
        StringBuilder builder = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            builder.append(
                    switch (text.charAt(i)) {
                        case 'ă', 'â' -> 'a';
                        case 'î' -> 'i';
                        case 'ș', 'ş' -> 's';
                        case 'ț', 'ţ' -> 't';
                        case 'Ă', 'Â' -> 'a';
                        case 'Î' -> 'i';
                        case 'Ș', 'Ş' -> 's';
                        case 'Ț', 'Ţ' -> 't';
                        default -> text.charAt(i);
                    }
            );
        }
        return builder.toString();
    }
}

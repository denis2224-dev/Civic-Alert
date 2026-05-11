package com.civicalert.util;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class ElectionTextRules {

    private static final Set<String> SMS_TOKENS = Set.of("sms", "смс");

    private static final Set<String> VOTE_TOKENS = Set.of(
            "vote",
            "voting",
            "vot",
            "vota",
            "votez",
            "votare",
            "голос",
            "голосовать",
            "голосование"
    );

    private ElectionTextRules() {
    }

    public static boolean isSmsVotingClaim(String normalizedText) {
        Set<String> tokens = tokenize(normalizedText);
        return containsSmsToken(tokens) && containsVoteToken(tokens);
    }

    public static boolean containsSmsToken(Set<String> tokens) {
        return tokens.stream().anyMatch(SMS_TOKENS::contains);
    }

    public static boolean containsVoteToken(Set<String> tokens) {
        for (String token : tokens) {
            if (VOTE_TOKENS.contains(token)
                    || token.startsWith("vote")
                    || token.startsWith("vot")
                    || token.startsWith("голос")) {
                return true;
            }
        }
        return false;
    }

    public static Set<String> tokenize(String normalizedText) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(normalizedText.split("\\s+"))
                .filter(token -> !token.isBlank())
                .collect(Collectors.toSet());
    }
}

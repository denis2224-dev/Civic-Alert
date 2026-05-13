package com.civicalert.service;

import com.civicalert.dto.ClaimCheckRequest;
import com.civicalert.dto.ClaimCheckResponse;
import com.civicalert.dto.DetectionResult;
import com.civicalert.entity.RumorPattern;
import com.civicalert.entity.VerifiedClaim;
import com.civicalert.enums.ClaimStatus;
import com.civicalert.enums.RiskLevel;
import com.civicalert.repository.RumorPatternRepository;
import com.civicalert.repository.VerifiedClaimRepository;
import com.civicalert.util.ElectionTextRules;
import com.civicalert.util.TextNormalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ClaimCheckService {

    private static final Set<String> FILLER_WORDS = Set.of(
            "can", "i", "we", "you", "is", "are", "the", "a", "an", "by", "with", "to", "today",
            "pot", "poti", "poate", "sa", "să", "eu", "noi", "oare", "este", "e", "prin", "cu", "la",
            "можно", "ли", "я", "мы", "это", "по", "через"
    );

    private static final Set<String> ELECTION_KEYWORDS = Set.of(
            "sms", "смс", "vote", "voting", "vot", "vota", "votez", "votare", "голос", "голосовать", "голосование",
            "election", "alegeri", "выборы", "polling", "sectia", "участок", "results", "rezultate", "результаты"
    );

    private final VerifiedClaimRepository verifiedClaimRepository;
    private final RumorPatternRepository rumorPatternRepository;
    private final CEngineService cEngineService;

    public ClaimCheckService(
            VerifiedClaimRepository verifiedClaimRepository,
            RumorPatternRepository rumorPatternRepository,
            CEngineService cEngineService
    ) {
        this.verifiedClaimRepository = verifiedClaimRepository;
        this.rumorPatternRepository = rumorPatternRepository;
        this.cEngineService = cEngineService;
    }

    public ClaimCheckResponse checkClaim(ClaimCheckRequest request) {
        String normalizedClaim = TextNormalizer.normalize(request.getText());
        String language = normalizeLanguage(request.getLanguage());
        boolean smsVotingClaim = ElectionTextRules.isSmsVotingClaim(normalizedClaim);

        List<VerifiedClaim> languageClaims = verifiedClaimRepository.findByPublishedTrueAndLanguageOrderByUpdatedAtDesc(language);
        List<VerifiedClaim> globalClaims = verifiedClaimRepository.findByPublishedTrueOrderByUpdatedAtDesc();

        Optional<VerifiedClaim> matchedClaim = findVerifiedClaimMatch(normalizedClaim, languageClaims, globalClaims);
        if (matchedClaim.isPresent()) {
            List<VerifiedClaim> smsCorrectionPool = !languageClaims.isEmpty() ? languageClaims : globalClaims;
            return buildVerifiedResponseWithSmsSafeguard(matchedClaim.get(), smsVotingClaim, language, smsCorrectionPool);
        }

        Optional<PatternMatch> dbPatternMatch = findRumorPatternMatch(normalizedClaim, language);
        if (dbPatternMatch.isPresent()) {
            if (smsVotingClaim) {
                List<VerifiedClaim> smsCorrectionPool = !languageClaims.isEmpty() ? languageClaims : globalClaims;
                return buildSmsRuleResponse(language, smsCorrectionPool);
            }
            return buildNeedsReviewResponse(
                    dbPatternMatch.get().pattern().getCategory(),
                    dbPatternMatch.get().riskLevel(),
                    "This claim matches a known election rumor pattern and needs validator review."
            );
        }

        if (smsVotingClaim) {
            List<VerifiedClaim> smsCorrectionPool = !languageClaims.isEmpty() ? languageClaims : globalClaims;
            return buildSmsRuleResponse(language, smsCorrectionPool);
        }

        DetectionResult detectionResult = cEngineService.analyzeClaim(normalizedClaim, language);
        if (detectionResult.getError() != null && !detectionResult.getError().isBlank()) {
            return buildNeedsReviewResponse(
                    "engine_unavailable",
                    RiskLevel.MEDIUM,
                    "The claim could not be fully analyzed by the detection engine, but it was received."
            );
        }

        if (detectionResult.isMatched()) {
            return buildNeedsReviewResponse(
                    detectionResult.getCategory(),
                    detectionResult.getRiskLevel() == null ? RiskLevel.MEDIUM : detectionResult.getRiskLevel(),
                    "This claim appears suspicious and needs validator review."
            );
        }

        return buildNoMatchResponse();
    }

    private ClaimCheckResponse buildVerifiedResponse(VerifiedClaim claim) {
        ClaimCheckResponse response = new ClaimCheckResponse();
        response.setStatus(claim.getStatus());
        response.setRiskLevel(mapRiskLevel(claim.getStatus()));
        response.setCategory(claim.getCategory());
        response.setMessage(mapMessage(claim.getStatus()));
        response.setCorrection(claim.getCorrectionText());
        response.setOfficialSource(claim.getOfficialSource());
        response.setOfficialSourceUrl(claim.getOfficialSourceUrl());
        response.setLastUpdated(claim.getUpdatedAt());
        return response;
    }

    private ClaimCheckResponse buildVerifiedResponseWithSmsSafeguard(
            VerifiedClaim claim,
            boolean smsVotingClaim,
            String language,
            List<VerifiedClaim> publishedClaims
    ) {
        if (!smsVotingClaim) {
            return buildVerifiedResponse(claim);
        }

        if (claim.getStatus() == ClaimStatus.VERIFIED_TRUE) {
            return buildSmsRuleResponse(language, publishedClaims);
        }

        ClaimCheckResponse response = buildVerifiedResponse(claim);
        response.setStatus(ClaimStatus.VERIFIED_FALSE);
        response.setCategory("fake_voting_method");
        response.setRiskLevel(RiskLevel.CRITICAL);
        response.setMessage("Voting by SMS is not available. Use only official voting procedures.");
        if (response.getCorrection() == null || response.getCorrection().isBlank()) {
            response.setCorrection(smsCorrectionForLanguage(language));
        }
        return response;
    }

    private ClaimCheckResponse buildNeedsReviewResponse(String category, RiskLevel riskLevel, String message) {
        ClaimCheckResponse response = new ClaimCheckResponse();
        response.setStatus(ClaimStatus.NEEDS_REVIEW);
        response.setRiskLevel(riskLevel);
        response.setCategory(category == null || category.isBlank() ? "unknown" : category);
        response.setMessage(message);
        return response;
    }

    private ClaimCheckResponse buildNoMatchResponse() {
        ClaimCheckResponse response = new ClaimCheckResponse();
        response.setStatus(ClaimStatus.NO_MATCH_FOUND);
        response.setRiskLevel(RiskLevel.LOW);
        response.setCategory("neutral");
        response.setMessage("No verified match found for election-process misinformation.");
        return response;
    }

    private ClaimCheckResponse buildSmsRuleResponse(String language, List<VerifiedClaim> publishedClaims) {
        Optional<VerifiedClaim> correctionClaim = findSmsCorrectionClaim(publishedClaims);
        if (correctionClaim.isPresent()) {
            ClaimCheckResponse response = buildVerifiedResponse(correctionClaim.get());
            response.setStatus(ClaimStatus.VERIFIED_FALSE);
            response.setCategory("fake_voting_method");
            response.setRiskLevel(RiskLevel.CRITICAL);
            response.setMessage("Voting by SMS is not available. Use only official voting procedures.");
            if (response.getCorrection() == null || response.getCorrection().isBlank()) {
                response.setCorrection(smsCorrectionForLanguage(language));
            }
            return response;
        }

        ClaimCheckResponse response = new ClaimCheckResponse();
        response.setStatus(ClaimStatus.NEEDS_REVIEW);
        response.setRiskLevel(RiskLevel.CRITICAL);
        response.setCategory("fake_voting_method");
        response.setMessage("This claim appears suspicious and needs validator review.");
        return response;
    }

    private Optional<VerifiedClaim> findSmsCorrectionClaim(List<VerifiedClaim> publishedClaims) {
        return publishedClaims.stream()
                .filter(claim -> ElectionTextRules.isSmsVotingClaim(claim.getNormalizedClaim()))
                .filter(claim -> claim.getStatus() == ClaimStatus.VERIFIED_FALSE || claim.getStatus() == ClaimStatus.MISLEADING)
                .findFirst()
                .or(() -> verifiedClaimRepository.findByPublishedTrueOrderByUpdatedAtDesc().stream()
                        .filter(claim -> ElectionTextRules.isSmsVotingClaim(claim.getNormalizedClaim()))
                        .filter(claim -> claim.getStatus() == ClaimStatus.VERIFIED_FALSE || claim.getStatus() == ClaimStatus.MISLEADING)
                        .findFirst());
    }

    private Optional<PatternMatch> findRumorPatternMatch(String normalizedClaim, String language) {
        List<RumorPattern> languagePatterns = rumorPatternRepository.findByActiveTrueAndLanguage(language);
        Optional<PatternMatch> languageMatch = findBestPatternMatch(normalizedClaim, languagePatterns);
        if (languageMatch.isPresent()) {
            return languageMatch;
        }

        List<RumorPattern> globalPatterns = rumorPatternRepository.findByActiveTrue();
        return findBestPatternMatch(normalizedClaim, globalPatterns);
    }

    private Optional<PatternMatch> findBestPatternMatch(String normalizedClaim, List<RumorPattern> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return Optional.empty();
        }

        return patterns.stream()
                .map(pattern -> evaluatePatternMatch(normalizedClaim, pattern))
                .flatMap(Optional::stream)
                .max(Comparator
                        .comparingInt(PatternMatch::riskScore)
                        .thenComparing(match -> match.pattern().getSeverity(), Comparator.nullsLast(Comparator.naturalOrder()))
                );
    }

    private Optional<VerifiedClaim> findVerifiedClaimMatch(
            String normalizedClaim,
            List<VerifiedClaim> languageClaims,
            List<VerifiedClaim> globalClaims
    ) {
        Optional<VerifiedClaim> exactLanguage = findExactMatch(normalizedClaim, languageClaims);
        if (exactLanguage.isPresent()) {
            return exactLanguage;
        }

        Optional<VerifiedClaim> exactGlobal = findExactMatch(normalizedClaim, globalClaims);
        if (exactGlobal.isPresent()) {
            return exactGlobal;
        }

        Optional<VerifiedClaim> similarLanguage = findSimilarMatch(normalizedClaim, languageClaims);
        if (similarLanguage.isPresent()) {
            return similarLanguage;
        }

        return findSimilarMatch(normalizedClaim, globalClaims);
    }

    private Optional<VerifiedClaim> findExactMatch(String normalizedClaim, List<VerifiedClaim> claims) {
        if (claims == null || claims.isEmpty()) {
            return Optional.empty();
        }
        return claims.stream()
                .filter(claim -> normalizedClaim.equals(claim.getNormalizedClaim()))
                .findFirst();
    }

    private Optional<VerifiedClaim> findSimilarMatch(String normalizedClaim, List<VerifiedClaim> claims) {
        if (claims == null || claims.isEmpty()) {
            return Optional.empty();
        }
        return claims.stream()
                .filter(claim -> isSimilarEnough(normalizedClaim, claim.getNormalizedClaim()))
                .findFirst();
    }

    private Optional<PatternMatch> evaluatePatternMatch(String normalizedClaim, RumorPattern pattern) {
        String normalizedPattern = pattern.getNormalizedPhrase();
        if (normalizedPattern == null || normalizedPattern.isBlank()) {
            return Optional.empty();
        }

        boolean exactMatch =
                normalizedClaim.contains(normalizedPattern) || normalizedPattern.contains(normalizedClaim);
        if (!exactMatch && !isSimilarEnough(normalizedClaim, normalizedPattern)) {
            return Optional.empty();
        }

        int riskScore = calculateRiskScore(pattern.getSeverity(), exactMatch, pattern.getCategory());
        RiskLevel riskLevel = riskLevelFromScore(riskScore);
        return Optional.of(new PatternMatch(pattern, riskScore, riskLevel));
    }

    private int calculateRiskScore(Integer severity, boolean exactMatch, String category) {
        int safeSeverity = severity == null ? 1 : Math.max(1, severity);
        int score = safeSeverity * 15;
        score += exactMatch ? 10 : 8;

        if ("voter_suppression".equals(category) || "fake_voting_method".equals(category)) {
            score += 10;
        } else if ("voting_date".equals(category) || "polling_location".equals(category)) {
            score += 5;
        }
        return score;
    }

    private RiskLevel riskLevelFromScore(int score) {
        if (score >= 80) {
            return RiskLevel.CRITICAL;
        }
        if (score >= 55) {
            return RiskLevel.HIGH;
        }
        if (score >= 30) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private boolean isSimilarEnough(String input, String savedClaim) {
        if (input == null || savedClaim == null || input.isBlank() || savedClaim.isBlank()) {
            return false;
        }
        if (input.equals(savedClaim)) {
            return true;
        }

        Set<String> inputTokens = tokenizeImportant(input);
        Set<String> savedTokens = tokenizeImportant(savedClaim);
        if (inputTokens.isEmpty() || savedTokens.isEmpty()) {
            return false;
        }

        if (ElectionTextRules.containsSmsToken(inputTokens)
                && ElectionTextRules.containsSmsToken(savedTokens)
                && ElectionTextRules.containsVoteToken(inputTokens)
                && ElectionTextRules.containsVoteToken(savedTokens)) {
            return true;
        }

        Set<String> shared = new HashSet<>(inputTokens);
        shared.retainAll(savedTokens);
        if (shared.isEmpty()) {
            return false;
        }

        double overlap = (double) shared.size() / (double) Math.max(inputTokens.size(), savedTokens.size());
        if (overlap >= 0.60) {
            return true;
        }

        if (shared.size() >= 2 && shared.stream().anyMatch(ELECTION_KEYWORDS::contains)) {
            return true;
        }

        if (shared.size() == 1) {
            String token = shared.iterator().next();
            return ELECTION_KEYWORDS.contains(token) && (inputTokens.size() <= 2 || savedTokens.size() <= 2);
        }

        return false;
    }

    private Set<String> tokenizeImportant(String text) {
        return Arrays.stream(text.split("\\s+"))
                .map(token -> token.trim().toLowerCase(Locale.ROOT))
                .filter(token -> !token.isBlank())
                .filter(token -> !FILLER_WORDS.contains(token))
                .collect(java.util.stream.Collectors.toSet());
    }

    private String smsCorrectionForLanguage(String language) {
        return switch (language) {
            case "ro" -> "Votarea prin SMS nu este disponibilă. Folosiți doar procedurile oficiale de vot.";
            case "ru" -> "Голосование по SMS недоступно. Используйте только официальные процедуры голосования.";
            default -> "Voting by SMS is not available. Use only official voting procedures.";
        };
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "en";
        }
        return language.trim().toLowerCase(Locale.ROOT);
    }

    private RiskLevel mapRiskLevel(ClaimStatus status) {
        return switch (status) {
            case VERIFIED_FALSE, MISLEADING -> RiskLevel.HIGH;
            case NEEDS_CONTEXT -> RiskLevel.MEDIUM;
            case VERIFIED_TRUE, NO_MATCH_FOUND, REJECTED -> RiskLevel.LOW;
            case NEEDS_REVIEW -> RiskLevel.MEDIUM;
        };
    }

    private String mapMessage(ClaimStatus status) {
        return switch (status) {
            case VERIFIED_TRUE -> "This claim is verified as true.";
            case VERIFIED_FALSE -> "This claim is misleading.";
            case MISLEADING -> "This claim is partially misleading.";
            case NEEDS_CONTEXT -> "This claim needs additional context.";
            case NEEDS_REVIEW -> "This claim needs validator review.";
            case NO_MATCH_FOUND -> "No known election-process rumor pattern was found.";
            case REJECTED -> "This claim was rejected by validators.";
        };
    }

    private record PatternMatch(RumorPattern pattern, int riskScore, RiskLevel riskLevel) {
    }
}

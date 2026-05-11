package com.civicalert.service;

import com.civicalert.dto.DetectionResult;
import com.civicalert.entity.RumorPattern;
import com.civicalert.enums.RiskLevel;
import com.civicalert.repository.RumorPatternRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class CEngineService {

    private final RumorPatternRepository rumorPatternRepository;

    public CEngineService(RumorPatternRepository rumorPatternRepository) {
        this.rumorPatternRepository = rumorPatternRepository;
    }

    public DetectionResult analyzeClaim(String normalizedClaim, String language) {
        DetectionResult result = new DetectionResult();
        result.setMatched(false);
        result.setRiskScore(0);
        result.setRiskLevel(RiskLevel.LOW);
        result.setEngineOutput("{\"matched\":false}");

        if (normalizedClaim == null || normalizedClaim.isBlank()) {
            return result;
        }

        String normalizedLanguage = language == null ? "" : language.trim().toLowerCase(Locale.ROOT);
        List<RumorPattern> patterns = rumorPatternRepository.findByActiveTrue();

        for (RumorPattern pattern : patterns) {
            if (!normalizedLanguage.isBlank() && !pattern.getLanguage().equalsIgnoreCase(normalizedLanguage)) {
                continue;
            }
            if (!normalizedClaim.contains(pattern.getNormalizedPhrase())) {
                continue;
            }

            int riskScore = calculateRiskScore(pattern.getSeverity(), pattern.getCategory(), true);
            result.setMatched(true);
            result.setMatchedPhrase(pattern.getPhrase());
            result.setCategory(pattern.getCategory());
            result.setSeverity(pattern.getSeverity());
            result.setRiskScore(riskScore);
            result.setRiskLevel(mapRiskLevel(riskScore));
            result.setEngineOutput(buildEngineOutput(result));
            return result;
        }

        return result;
    }

    private int calculateRiskScore(int severity, String category, boolean matched) {
        int score = severity * 15;
        if (matched) {
            score += 10;
        }
        if ("voter_suppression".equalsIgnoreCase(category) || "fake_voting_method".equalsIgnoreCase(category)) {
            score += 10;
        }
        return score;
    }

    private RiskLevel mapRiskLevel(int score) {
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

    private String buildEngineOutput(DetectionResult result) {
        return String.format(
                Locale.ROOT,
                "{\"matched\":true,\"matchedPhrase\":\"%s\",\"category\":\"%s\",\"severity\":%d,\"riskScore\":%d,\"riskLevel\":\"%s\"}",
                escapeJson(result.getMatchedPhrase()),
                escapeJson(result.getCategory()),
                result.getSeverity(),
                result.getRiskScore(),
                result.getRiskLevel().name()
        );
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}


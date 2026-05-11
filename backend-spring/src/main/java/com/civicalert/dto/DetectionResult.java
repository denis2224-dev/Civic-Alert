package com.civicalert.dto;

import com.civicalert.enums.RiskLevel;

public class DetectionResult {

    private boolean matched;
    private String matchedPhrase;
    private String category;
    private Integer severity;
    private Integer riskScore;
    private RiskLevel riskLevel;
    private String engineOutput;
    private String error;

    public boolean isMatched() {
        return matched;
    }

    public void setMatched(boolean matched) {
        this.matched = matched;
    }

    public String getMatchedPhrase() {
        return matchedPhrase;
    }

    public void setMatchedPhrase(String matchedPhrase) {
        this.matchedPhrase = matchedPhrase;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getSeverity() {
        return severity;
    }

    public void setSeverity(Integer severity) {
        this.severity = severity;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getEngineOutput() {
        return engineOutput;
    }

    public void setEngineOutput(String engineOutput) {
        this.engineOutput = engineOutput;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}


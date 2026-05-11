package com.civicalert.dto;

import com.civicalert.enums.ClaimStatus;
import com.civicalert.enums.RiskLevel;
import java.time.LocalDateTime;

public class ClaimCheckResponse {

    private ClaimStatus status;
    private RiskLevel riskLevel;
    private String category;
    private String message;
    private String correction;
    private String officialSource;
    private String officialSourceUrl;
    private LocalDateTime lastUpdated;

    public ClaimStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimStatus status) {
        this.status = status;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCorrection() {
        return correction;
    }

    public void setCorrection(String correction) {
        this.correction = correction;
    }

    public String getOfficialSource() {
        return officialSource;
    }

    public void setOfficialSource(String officialSource) {
        this.officialSource = officialSource;
    }

    public String getOfficialSourceUrl() {
        return officialSourceUrl;
    }

    public void setOfficialSourceUrl(String officialSourceUrl) {
        this.officialSourceUrl = officialSourceUrl;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}


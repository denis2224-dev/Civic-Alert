package com.civicalert.dto;

import com.civicalert.enums.ClaimStatus;
import java.time.LocalDateTime;

public class VerifiedClaimResponse {

    private Long id;
    private String claimText;
    private String category;
    private ClaimStatus status;
    private String correctionText;
    private String officialSource;
    private String officialSourceUrl;
    private String language;
    private String region;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClaimText() {
        return claimText;
    }

    public void setClaimText(String claimText) {
        this.claimText = claimText;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimStatus status) {
        this.status = status;
    }

    public String getCorrectionText() {
        return correctionText;
    }

    public void setCorrectionText(String correctionText) {
        this.correctionText = correctionText;
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

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}


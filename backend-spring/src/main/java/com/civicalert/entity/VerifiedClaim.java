package com.civicalert.entity;

import com.civicalert.enums.ClaimStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "verified_claims")
public class VerifiedClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "claim_text", nullable = false, columnDefinition = "TEXT")
    private String claimText;

    @Column(name = "normalized_claim", nullable = false, columnDefinition = "TEXT")
    private String normalizedClaim;

    @Column(nullable = false)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus status;

    @Column(name = "correction_text", columnDefinition = "TEXT")
    private String correctionText;

    @Column(name = "official_source")
    private String officialSource;

    @Column(name = "official_source_url")
    private String officialSourceUrl;

    @Column(nullable = false)
    private String language;

    @Column(nullable = false)
    private String region;

    @Column(nullable = false)
    private Boolean published = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

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

    public String getNormalizedClaim() {
        return normalizedClaim;
    }

    public void setNormalizedClaim(String normalizedClaim) {
        this.normalizedClaim = normalizedClaim;
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

    public Boolean getPublished() {
        return published;
    }

    public void setPublished(Boolean published) {
        this.published = published;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}


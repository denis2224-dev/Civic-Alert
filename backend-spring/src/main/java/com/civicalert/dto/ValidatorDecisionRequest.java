package com.civicalert.dto;

import com.civicalert.enums.ClaimStatus;
import jakarta.validation.constraints.NotNull;

public class ValidatorDecisionRequest {

    @NotNull
    private ClaimStatus status;

    private String correctionText;

    private String officialSource;

    private String officialSourceUrl;

    private boolean publish;

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

    public boolean isPublish() {
        return publish;
    }

    public void setPublish(boolean publish) {
        this.publish = publish;
    }
}


package com.civicalert.dto;

import jakarta.validation.constraints.NotBlank;

public class ClaimCheckRequest {

    @NotBlank
    private String text;

    @NotBlank
    private String region;

    @NotBlank
    private String language;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}


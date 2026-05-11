package com.civicalert.service;

import com.civicalert.dto.VerifiedClaimResponse;
import com.civicalert.entity.VerifiedClaim;
import com.civicalert.repository.VerifiedClaimRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class VerifiedClaimService {

    private final VerifiedClaimRepository verifiedClaimRepository;

    public VerifiedClaimService(VerifiedClaimRepository verifiedClaimRepository) {
        this.verifiedClaimRepository = verifiedClaimRepository;
    }

    public List<VerifiedClaimResponse> getPublishedClaims(String category, String language) {
        List<VerifiedClaim> claims;
        if (language != null && !language.isBlank()) {
            claims = verifiedClaimRepository.findByPublishedTrueAndLanguageOrderByUpdatedAtDesc(language.trim().toLowerCase(Locale.ROOT));
        } else {
            claims = verifiedClaimRepository.findByPublishedTrueOrderByUpdatedAtDesc();
        }

        return claims.stream()
                .filter(claim -> category == null
                        || category.isBlank()
                        || claim.getCategory().equalsIgnoreCase(category.trim()))
                .map(this::toResponse)
                .toList();
    }

    private VerifiedClaimResponse toResponse(VerifiedClaim claim) {
        VerifiedClaimResponse response = new VerifiedClaimResponse();
        response.setId(claim.getId());
        response.setClaimText(claim.getClaimText());
        response.setCategory(claim.getCategory());
        response.setStatus(claim.getStatus());
        response.setCorrectionText(claim.getCorrectionText());
        response.setOfficialSource(claim.getOfficialSource());
        response.setOfficialSourceUrl(claim.getOfficialSourceUrl());
        response.setLanguage(claim.getLanguage());
        response.setRegion(claim.getRegion());
        response.setUpdatedAt(claim.getUpdatedAt());
        return response;
    }
}


package com.civicalert.service;

import com.civicalert.dto.ClaimCheckRequest;
import com.civicalert.dto.ClaimCheckResponse;
import com.civicalert.dto.DetectionResult;
import com.civicalert.entity.VerifiedClaim;
import com.civicalert.enums.ClaimStatus;
import com.civicalert.enums.RiskLevel;
import com.civicalert.repository.VerifiedClaimRepository;
import com.civicalert.util.TextNormalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ClaimCheckService {

    private final VerifiedClaimRepository verifiedClaimRepository;
    private final CEngineService cEngineService;

    public ClaimCheckService(
            VerifiedClaimRepository verifiedClaimRepository,
            CEngineService cEngineService
    ) {
        this.verifiedClaimRepository = verifiedClaimRepository;
        this.cEngineService = cEngineService;
    }

    public ClaimCheckResponse checkClaim(ClaimCheckRequest request) {
        String normalizedClaim = TextNormalizer.normalize(request.getText());
        String language = request.getLanguage().trim().toLowerCase(Locale.ROOT);

        Optional<VerifiedClaim> exactMatch =
                verifiedClaimRepository.findFirstByNormalizedClaimAndLanguageAndPublishedTrue(normalizedClaim, language);

        if (exactMatch.isPresent()) {
            return buildVerifiedResponse(exactMatch.get());
        }

        List<VerifiedClaim> publishedClaims = verifiedClaimRepository.findByPublishedTrueAndLanguageOrderByUpdatedAtDesc(language);
        Optional<VerifiedClaim> similarMatch = publishedClaims.stream()
                .filter(claim -> normalizedClaim.contains(claim.getNormalizedClaim())
                        || claim.getNormalizedClaim().contains(normalizedClaim))
                .findFirst();

        if (similarMatch.isPresent()) {
            return buildVerifiedResponse(similarMatch.get());
        }

        DetectionResult detectionResult = cEngineService.analyzeClaim(normalizedClaim, language);
        ClaimCheckResponse response = new ClaimCheckResponse();

        if (detectionResult.getError() != null && !detectionResult.getError().isBlank()) {
            response.setStatus(ClaimStatus.NEEDS_REVIEW);
            response.setRiskLevel(RiskLevel.MEDIUM);
            response.setCategory("engine_unavailable");
            response.setMessage("The claim could not be fully analyzed by the detection engine, but it was received.");
        } else if (detectionResult.isMatched()) {
            response.setStatus(ClaimStatus.NEEDS_REVIEW);
            response.setRiskLevel(detectionResult.getRiskLevel());
            response.setCategory(detectionResult.getCategory());
            response.setMessage("This claim appears suspicious and needs validator review.");
        } else {
            response.setStatus(ClaimStatus.NO_MATCH_FOUND);
            response.setRiskLevel(RiskLevel.LOW);
            response.setCategory("general");
            response.setMessage("No known election-process rumor pattern was found.");
        }
        return response;
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
}

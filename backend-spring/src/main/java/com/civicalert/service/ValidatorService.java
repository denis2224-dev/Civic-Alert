package com.civicalert.service;

import com.civicalert.dto.ValidatorDecisionRequest;
import com.civicalert.entity.DetectionLog;
import com.civicalert.entity.PublicReport;
import com.civicalert.entity.VerifiedClaim;
import com.civicalert.enums.ClaimStatus;
import com.civicalert.enums.ReportStatus;
import com.civicalert.enums.RiskLevel;
import com.civicalert.repository.DetectionLogRepository;
import com.civicalert.repository.PublicReportRepository;
import com.civicalert.repository.VerifiedClaimRepository;
import com.civicalert.util.ElectionTextRules;
import com.civicalert.util.TextNormalizer;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ValidatorService {

    private final PublicReportRepository publicReportRepository;
    private final DetectionLogRepository detectionLogRepository;
    private final VerifiedClaimRepository verifiedClaimRepository;

    public ValidatorService(
            PublicReportRepository publicReportRepository,
            DetectionLogRepository detectionLogRepository,
            VerifiedClaimRepository verifiedClaimRepository
    ) {
        this.publicReportRepository = publicReportRepository;
        this.detectionLogRepository = detectionLogRepository;
        this.verifiedClaimRepository = verifiedClaimRepository;
    }

    public List<PublicReport> getAllReportsOrdered() {
        return publicReportRepository.findAll().stream()
                .sorted(buildRiskComparator())
                .toList();
    }

    public Map<String, Object> getReportDetails(Long id) {
        PublicReport report = findReport(id);
        List<DetectionLog> detectionLogs = detectionLogRepository.findByReportIdOrderByCreatedAtDesc(id);

        List<Map<String, Object>> logItems = detectionLogs.stream()
                .map(log -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", log.getId());
                    item.put("matchedPhrase", log.getMatchedPhrase());
                    item.put("category", log.getCategory());
                    item.put("severity", log.getSeverity());
                    item.put("riskScore", log.getRiskScore());
                    item.put("riskLevel", log.getRiskLevel());
                    item.put("engineOutput", log.getEngineOutput());
                    item.put("createdAt", log.getCreatedAt());
                    return item;
                })
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("report", report);
        response.put("detectionLogs", logItems);
        return response;
    }

    @Transactional
    public PublicReport markUnderReview(Long id) {
        PublicReport report = findReport(id);
        report.setStatus(ReportStatus.UNDER_REVIEW);
        return publicReportRepository.save(report);
    }

    @Transactional
    public PublicReport applyDecision(Long id, ValidatorDecisionRequest request) {
        PublicReport report = findReport(id);
        guardAgainstInvalidSmsPublish(report, request);
        report.setStatus(resolveReportStatus(request.getStatus(), request.isPublish()));

        if (request.isPublish()) {
            upsertVerifiedClaim(report, request);
        }

        return publicReportRepository.save(report);
    }

    private void guardAgainstInvalidSmsPublish(PublicReport report, ValidatorDecisionRequest request) {
        if (!request.isPublish() || request.getStatus() != ClaimStatus.VERIFIED_TRUE) {
            return;
        }

        String normalizedClaim = TextNormalizer.normalize(report.getClaimText());
        if (ElectionTextRules.isSmsVotingClaim(normalizedClaim)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "SMS voting cannot be marked as verified true in this demo because official info says SMS voting is not available."
            );
        }
    }

    private PublicReport findReport(Long id) {
        return publicReportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
    }

    private void upsertVerifiedClaim(PublicReport report, ValidatorDecisionRequest request) {
        String normalizedClaim = TextNormalizer.normalize(report.getClaimText());
        String language = report.getLanguage().trim().toLowerCase(Locale.ROOT);

        VerifiedClaim claim = verifiedClaimRepository
                .findFirstByNormalizedClaimAndLanguageAndPublishedTrue(normalizedClaim, language)
                .orElseGet(VerifiedClaim::new);

        claim.setClaimText(report.getClaimText());
        claim.setNormalizedClaim(normalizedClaim);
        claim.setCategory(report.getCategory() == null || report.getCategory().isBlank()
                ? "validator_review"
                : report.getCategory());
        claim.setStatus(request.getStatus());
        claim.setCorrectionText(request.getCorrectionText());
        claim.setOfficialSource(request.getOfficialSource());
        claim.setOfficialSourceUrl(request.getOfficialSourceUrl());
        claim.setLanguage(language);
        claim.setRegion(report.getRegion());
        claim.setPublished(true);

        verifiedClaimRepository.save(claim);
    }

    private ReportStatus resolveReportStatus(ClaimStatus claimStatus, boolean publish) {
        if (claimStatus == ClaimStatus.REJECTED) {
            return ReportStatus.REJECTED;
        }
        if (publish) {
            return ReportStatus.PUBLISHED;
        }
        return ReportStatus.RESOLVED;
    }

    private Comparator<PublicReport> buildRiskComparator() {
        return Comparator
                .comparingInt((PublicReport report) -> riskPriority(report.getRiskLevel())).reversed()
                .thenComparing(PublicReport::getRiskScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(PublicReport::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private int riskPriority(RiskLevel riskLevel) {
        if (riskLevel == null) {
            return 0;
        }
        return switch (riskLevel) {
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
            case CRITICAL -> 4;
        };
    }
}

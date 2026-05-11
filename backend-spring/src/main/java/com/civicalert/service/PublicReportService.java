package com.civicalert.service;

import com.civicalert.dto.DetectionResult;
import com.civicalert.dto.ReportRequest;
import com.civicalert.dto.ReportResponse;
import com.civicalert.dto.ReportStatusResponse;
import com.civicalert.entity.DetectionLog;
import com.civicalert.entity.PublicReport;
import com.civicalert.enums.ReportStatus;
import com.civicalert.repository.DetectionLogRepository;
import com.civicalert.repository.PublicReportRepository;
import com.civicalert.util.TextNormalizer;
import com.civicalert.util.TrackingCodeGenerator;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PublicReportService {

    private final PublicReportRepository publicReportRepository;
    private final DetectionLogRepository detectionLogRepository;
    private final CEngineService cEngineService;

    public PublicReportService(
            PublicReportRepository publicReportRepository,
            DetectionLogRepository detectionLogRepository,
            CEngineService cEngineService
    ) {
        this.publicReportRepository = publicReportRepository;
        this.detectionLogRepository = detectionLogRepository;
        this.cEngineService = cEngineService;
    }

    public ReportResponse submitReport(ReportRequest request) {
        String normalizedText = TextNormalizer.normalize(request.getText());
        String language = request.getLanguage().trim().toLowerCase(Locale.ROOT);

        DetectionResult detection = cEngineService.analyzeClaim(normalizedText, language);

        PublicReport report = new PublicReport();
        report.setTrackingCode(generateTrackingCode());
        report.setClaimText(request.getText().trim());
        report.setNormalizedText(normalizedText);
        report.setPlatform(request.getPlatform().trim());
        report.setSourceUrl(request.getSourceUrl());
        report.setRegion(request.getRegion().trim());
        report.setLanguage(language);
        report.setStatus(ReportStatus.RECEIVED);
        report.setCategory(detection.getCategory());
        report.setRiskLevel(detection.getRiskLevel());
        report.setRiskScore(detection.getRiskScore());
        report.setMatchedPhrase(detection.getMatchedPhrase());
        PublicReport savedReport = publicReportRepository.save(report);

        DetectionLog log = new DetectionLog();
        log.setReport(savedReport);
        log.setMatchedPhrase(detection.getMatchedPhrase());
        log.setCategory(detection.getCategory());
        log.setSeverity(detection.getSeverity());
        log.setRiskScore(detection.getRiskScore());
        log.setRiskLevel(detection.getRiskLevel());
        log.setEngineOutput(detection.getEngineOutput());
        detectionLogRepository.save(log);

        ReportResponse response = new ReportResponse();
        response.setTrackingCode(savedReport.getTrackingCode());
        response.setStatus(savedReport.getStatus());
        response.setMessage("Your report was received and may be reviewed.");
        return response;
    }

    public ReportStatusResponse findByTrackingCode(String trackingCode) {
        PublicReport report = publicReportRepository.findByTrackingCode(trackingCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tracking code not found"));

        ReportStatusResponse response = new ReportStatusResponse();
        response.setTrackingCode(report.getTrackingCode());
        response.setStatus(report.getStatus());
        response.setCategory(report.getCategory());
        response.setRiskLevel(report.getRiskLevel());
        response.setRiskScore(report.getRiskScore());
        response.setCreatedAt(report.getCreatedAt());
        response.setUpdatedAt(report.getUpdatedAt());
        return response;
    }

    private String generateTrackingCode() {
        int year = LocalDate.now().getYear();
        long sequence = publicReportRepository.count() + 1;
        String code = TrackingCodeGenerator.generate(year, sequence);
        while (publicReportRepository.findByTrackingCode(code).isPresent()) {
            sequence++;
            code = TrackingCodeGenerator.generate(year, sequence);
        }
        return code;
    }
}


package com.civicalert.controller;

import com.civicalert.dto.ReportRequest;
import com.civicalert.dto.ReportResponse;
import com.civicalert.dto.ReportStatusResponse;
import com.civicalert.service.PublicReportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicReportController {

    private final PublicReportService publicReportService;

    public PublicReportController(PublicReportService publicReportService) {
        this.publicReportService = publicReportService;
    }

    @PostMapping("/reports")
    public ReportResponse createReport(@Valid @RequestBody ReportRequest request) {
        return publicReportService.submitReport(request);
    }

    @GetMapping("/reports/{trackingCode}")
    public ReportStatusResponse getReportStatus(@PathVariable String trackingCode) {
        return publicReportService.findByTrackingCode(trackingCode);
    }
}


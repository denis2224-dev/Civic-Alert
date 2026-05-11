package com.civicalert.controller;

import com.civicalert.dto.ValidatorDecisionRequest;
import com.civicalert.entity.PublicReport;
import com.civicalert.service.ValidatorService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/validator")
public class ValidatorController {

    private final ValidatorService validatorService;

    public ValidatorController(ValidatorService validatorService) {
        this.validatorService = validatorService;
    }

    @GetMapping("/reports")
    public List<PublicReport> getReports() {
        return validatorService.getAllReportsOrdered();
    }

    @GetMapping("/reports/{id}")
    public Map<String, Object> getReportDetails(@PathVariable Long id) {
        return validatorService.getReportDetails(id);
    }

    @PatchMapping("/reports/{id}/under-review")
    public PublicReport markUnderReview(@PathVariable Long id) {
        return validatorService.markUnderReview(id);
    }

    @PostMapping("/reports/{id}/decision")
    public PublicReport decision(@PathVariable Long id, @Valid @RequestBody ValidatorDecisionRequest request) {
        return validatorService.applyDecision(id, request);
    }
}


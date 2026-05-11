package com.civicalert.controller;

import com.civicalert.dto.ClaimCheckRequest;
import com.civicalert.dto.ClaimCheckResponse;
import com.civicalert.service.ClaimCheckService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicClaimController {

    private final ClaimCheckService claimCheckService;

    public PublicClaimController(ClaimCheckService claimCheckService) {
        this.claimCheckService = claimCheckService;
    }

    @PostMapping("/check-claim")
    public ClaimCheckResponse checkClaim(@Valid @RequestBody ClaimCheckRequest request) {
        return claimCheckService.checkClaim(request);
    }
}


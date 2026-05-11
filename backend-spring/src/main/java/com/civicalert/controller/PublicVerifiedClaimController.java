package com.civicalert.controller;

import com.civicalert.dto.VerifiedClaimResponse;
import com.civicalert.service.VerifiedClaimService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicVerifiedClaimController {

    private final VerifiedClaimService verifiedClaimService;

    public PublicVerifiedClaimController(VerifiedClaimService verifiedClaimService) {
        this.verifiedClaimService = verifiedClaimService;
    }

    @GetMapping("/verified-claims")
    public List<VerifiedClaimResponse> getVerifiedClaims(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String language
    ) {
        return verifiedClaimService.getPublishedClaims(category, language);
    }
}


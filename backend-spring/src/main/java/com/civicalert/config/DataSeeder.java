package com.civicalert.config;

import com.civicalert.entity.OfficialInfo;
import com.civicalert.entity.RumorPattern;
import com.civicalert.entity.VerifiedClaim;
import com.civicalert.enums.ClaimStatus;
import com.civicalert.repository.OfficialInfoRepository;
import com.civicalert.repository.RumorPatternRepository;
import com.civicalert.repository.VerifiedClaimRepository;
import com.civicalert.util.TextNormalizer;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final OfficialInfoRepository officialInfoRepository;
    private final RumorPatternRepository rumorPatternRepository;
    private final VerifiedClaimRepository verifiedClaimRepository;

    public DataSeeder(
            OfficialInfoRepository officialInfoRepository,
            RumorPatternRepository rumorPatternRepository,
            VerifiedClaimRepository verifiedClaimRepository
    ) {
        this.officialInfoRepository = officialInfoRepository;
        this.rumorPatternRepository = rumorPatternRepository;
        this.verifiedClaimRepository = verifiedClaimRepository;
    }

    @Override
    public void run(String... args) {
        seedOfficialInfo();
        seedRumorPatterns();
        seedVerifiedClaims();
    }

    private void seedOfficialInfo() {
        seedOfficial("voting_hours", "Polling stations are open from 07:00 to 21:00.");
        seedOfficial("sms_voting", "Voting by SMS is not available.");
        seedOfficial("online_voting", "Online voting is not available.");
        seedOfficial("election_cancelled", "The election has not been cancelled.");
        seedOfficial(
                "polling_station",
                "Polling station information must be checked only through official sources."
        );
    }

    private void seedOfficial(String topic, String content) {
        if (officialInfoRepository.findByTopic(topic).isPresent()) {
            return;
        }

        OfficialInfo info = new OfficialInfo();
        info.setTopic(topic);
        info.setContent(content);
        info.setSourceName("Central Electoral Commission");
        info.setSourceUrl("https://example.com");
        info.setLanguage("en");
        officialInfoRepository.save(info);
    }

    private void seedRumorPatterns() {
        List<PatternSeed> patterns = List.of(
                new PatternSeed("voting cancelled", "voting_process", 5),
                new PatternSeed("election postponed", "voting_date", 5),
                new PatternSeed("polling station moved", "polling_location", 4),
                new PatternSeed("vote by sms", "fake_voting_method", 5),
                new PatternSeed("vote online", "fake_voting_method", 5),
                new PatternSeed("ballots invalid", "ballot_confusion", 4),
                new PatternSeed("do not vote today", "voter_suppression", 5),
                new PatternSeed("voting date changed", "voting_date", 5),
                new PatternSeed("polling stations are closed", "polling_location", 5)
        );

        for (PatternSeed pattern : patterns) {
            String normalized = TextNormalizer.normalize(pattern.phrase());
            if (rumorPatternRepository.findByNormalizedPhrase(normalized).isPresent()) {
                continue;
            }

            RumorPattern entity = new RumorPattern();
            entity.setPhrase(pattern.phrase());
            entity.setNormalizedPhrase(normalized);
            entity.setCategory(pattern.category());
            entity.setSeverity(pattern.severity());
            entity.setLanguage("en");
            entity.setActive(true);
            rumorPatternRepository.save(entity);
        }
    }

    private void seedVerifiedClaims() {
        seedVerifiedClaim(
                "You can vote by SMS.",
                "fake_voting_method",
                ClaimStatus.VERIFIED_FALSE,
                "Voting by SMS is not available.",
                "en",
                "Moldova"
        );
        seedVerifiedClaim(
                "The election was cancelled.",
                "voting_process",
                ClaimStatus.VERIFIED_FALSE,
                "The election has not been cancelled.",
                "en",
                "Moldova"
        );
        seedVerifiedClaim(
                "The polling station moved.",
                "polling_location",
                ClaimStatus.NEEDS_CONTEXT,
                "Polling station location changes must be checked only through official channels.",
                "en",
                "Moldova"
        );
    }

    private void seedVerifiedClaim(
            String claimText,
            String category,
            ClaimStatus status,
            String correctionText,
            String language,
            String region
    ) {
        String normalized = TextNormalizer.normalize(claimText);
        if (verifiedClaimRepository.findFirstByNormalizedClaimAndLanguageAndPublishedTrue(normalized, language).isPresent()) {
            return;
        }

        VerifiedClaim claim = new VerifiedClaim();
        claim.setClaimText(claimText);
        claim.setNormalizedClaim(normalized);
        claim.setCategory(category);
        claim.setStatus(status);
        claim.setCorrectionText(correctionText);
        claim.setOfficialSource("Central Electoral Commission");
        claim.setOfficialSourceUrl("https://example.com");
        claim.setLanguage(language);
        claim.setRegion(region);
        claim.setPublished(true);
        verifiedClaimRepository.save(claim);
    }

    private record PatternSeed(String phrase, String category, int severity) {
    }
}


package com.civicalert.config;

import com.civicalert.entity.OfficialInfo;
import com.civicalert.entity.RumorPattern;
import com.civicalert.entity.VerifiedClaim;
import com.civicalert.enums.ClaimStatus;
import com.civicalert.repository.OfficialInfoRepository;
import com.civicalert.repository.RumorPatternRepository;
import com.civicalert.repository.VerifiedClaimRepository;
import com.civicalert.util.ElectionTextRules;
import com.civicalert.util.TextNormalizer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final List<Path> SEED_FILE_CANDIDATES = List.of(
            Paths.get("civicalert_seed_data_requirements.txt"),
            Paths.get("../civicalert_seed_data_requirements.txt")
    );

    private final OfficialInfoRepository officialInfoRepository;
    private final RumorPatternRepository rumorPatternRepository;
    private final VerifiedClaimRepository verifiedClaimRepository;
    private final JdbcTemplate jdbcTemplate;

    public DataSeeder(
            OfficialInfoRepository officialInfoRepository,
            RumorPatternRepository rumorPatternRepository,
            VerifiedClaimRepository verifiedClaimRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.officialInfoRepository = officialInfoRepository;
        this.rumorPatternRepository = rumorPatternRepository;
        this.verifiedClaimRepository = verifiedClaimRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        ensureOfficialInfoTopicLanguageUniqueness();
        SeedData seedData = loadSeedData();
        seedOfficialInfo(seedData.officialInfo());
        seedRumorPatterns(seedData.rumorPatterns());
        seedVerifiedClaims(seedData.verifiedClaims());
        cleanupSmsVotingClaims();
    }

    private void ensureOfficialInfoTopicLanguageUniqueness() {
        try {
            String constraintName = jdbcTemplate.query(
                    """
                    SELECT con.conname
                    FROM pg_constraint con
                    JOIN pg_class rel ON rel.oid = con.conrelid
                    WHERE rel.relname = 'official_info'
                      AND con.contype = 'u'
                      AND pg_get_constraintdef(con.oid) ILIKE '%(topic)%'
                      AND pg_get_constraintdef(con.oid) NOT ILIKE '%(topic, language)%'
                    LIMIT 1
                    """,
                    rs -> rs.next() ? rs.getString(1) : null
            );

            if (constraintName != null && !constraintName.isBlank()) {
                jdbcTemplate.execute("ALTER TABLE official_info DROP CONSTRAINT \"" + constraintName.replace("\"", "\"\"") + "\"");
            }
            jdbcTemplate.execute(
                    "CREATE UNIQUE INDEX IF NOT EXISTS uk_official_info_topic_language ON official_info (topic, language)"
            );
        } catch (DataAccessException ex) {
            log.warn("Could not update official_info uniqueness constraint. Continuing with existing schema.", ex);
        }
    }

    private SeedData loadSeedData() {
        Path seedFile = resolveSeedFilePath()
                .orElseThrow(() -> new IllegalStateException(
                        "Seed data file not found. Expected civicalert_seed_data_requirements.txt in project root."
                ));

        List<OfficialSeed> officialSeeds = new ArrayList<>();
        List<RumorPatternSeed> rumorPatternSeeds = new ArrayList<>();
        List<VerifiedClaimSeed> verifiedClaimSeeds = new ArrayList<>();

        SeedSection currentSection = SeedSection.NONE;
        List<String> lines;
        try {
            lines = Files.readAllLines(seedFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read seed data file: " + seedFile, e);
        }

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.startsWith("## ")) {
                currentSection = SeedSection.fromHeader(line);
                continue;
            }
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (currentSection == SeedSection.NONE || currentSection == SeedSection.REQUIRED_DEMO_TESTS) {
                continue;
            }

            switch (currentSection) {
                case OFFICIAL_INFO -> parseOfficialInfo(line).ifPresent(officialSeeds::add);
                case RUMOR_PATTERNS -> parseRumorPattern(line).ifPresent(rumorPatternSeeds::add);
                case VERIFIED_CLAIMS -> parseVerifiedClaim(line).ifPresent(verifiedClaimSeeds::add);
                case NONE, REQUIRED_DEMO_TESTS -> {
                }
            }
        }

        log.info(
                "Loaded seed data file {} with {} official records, {} rumor patterns, {} verified claims.",
                seedFile,
                officialSeeds.size(),
                rumorPatternSeeds.size(),
                verifiedClaimSeeds.size()
        );
        return new SeedData(officialSeeds, rumorPatternSeeds, verifiedClaimSeeds);
    }

    private Optional<Path> resolveSeedFilePath() {
        for (Path candidate : SEED_FILE_CANDIDATES) {
            Path path = candidate.toAbsolutePath().normalize();
            if (Files.exists(path) && Files.isRegularFile(path)) {
                return Optional.of(path);
            }
        }
        return Optional.empty();
    }

    private Optional<OfficialSeed> parseOfficialInfo(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length != 5) {
            log.warn("Skipping malformed OFFICIAL_INFO line: {}", line);
            return Optional.empty();
        }
        return Optional.of(
                new OfficialSeed(
                        parts[0].trim(),
                        parts[1].trim(),
                        parts[2].trim(),
                        blankToNull(parts[3]),
                        parts[4].trim().toLowerCase(Locale.ROOT)
                )
        );
    }

    private Optional<RumorPatternSeed> parseRumorPattern(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length != 4) {
            log.warn("Skipping malformed RUMOR_PATTERNS line: {}", line);
            return Optional.empty();
        }
        try {
            return Optional.of(
                    new RumorPatternSeed(
                            parts[0].trim(),
                            parts[1].trim(),
                            Integer.parseInt(parts[2].trim()),
                            parts[3].trim().toLowerCase(Locale.ROOT)
                    )
            );
        } catch (NumberFormatException ex) {
            log.warn("Skipping RUMOR_PATTERNS line with invalid severity: {}", line);
            return Optional.empty();
        }
    }

    private Optional<VerifiedClaimSeed> parseVerifiedClaim(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length != 9) {
            log.warn("Skipping malformed VERIFIED_CLAIMS line: {}", line);
            return Optional.empty();
        }

        ClaimStatus status;
        try {
            status = ClaimStatus.valueOf(parts[2].trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            log.warn("Skipping VERIFIED_CLAIMS line with invalid status: {}", line);
            return Optional.empty();
        }

        return Optional.of(
                new VerifiedClaimSeed(
                        parts[0].trim(),
                        parts[1].trim(),
                        status,
                        parts[3].trim(),
                        parts[4].trim(),
                        blankToNull(parts[5]),
                        parts[6].trim().toLowerCase(Locale.ROOT),
                        blankToNull(parts[7]),
                        Boolean.parseBoolean(parts[8].trim())
                )
        );
    }

    private void seedOfficialInfo(List<OfficialSeed> officialSeeds) {
        int inserted = 0;
        for (OfficialSeed officialSeed : officialSeeds) {
            Optional<OfficialInfo> existing = officialInfoRepository.findByTopicAndLanguage(
                    officialSeed.topic(),
                    officialSeed.language()
            );
            if (existing.isPresent()) {
                continue;
            }

            OfficialInfo info = new OfficialInfo();
            info.setTopic(officialSeed.topic());
            info.setContent(officialSeed.content());
            info.setSourceName(
                    officialSeed.sourceName().isBlank()
                            ? "Demo Central Electoral Commission"
                            : officialSeed.sourceName()
            );
            info.setSourceUrl(officialSeed.sourceUrl());
            info.setLanguage(officialSeed.language());
            officialInfoRepository.save(info);
            inserted++;
        }
        log.info("Seeded official_info: inserted {} new rows.", inserted);
    }

    private void seedRumorPatterns(List<RumorPatternSeed> patternSeeds) {
        int inserted = 0;
        for (RumorPatternSeed patternSeed : patternSeeds) {
            String normalizedPhrase = TextNormalizer.normalize(patternSeed.phrase());
            if (rumorPatternRepository
                    .findByNormalizedPhraseAndCategoryAndLanguage(
                            normalizedPhrase,
                            patternSeed.category(),
                            patternSeed.language()
                    )
                    .isPresent()) {
                continue;
            }

            RumorPattern entity = new RumorPattern();
            entity.setPhrase(patternSeed.phrase());
            entity.setNormalizedPhrase(normalizedPhrase);
            entity.setCategory(patternSeed.category());
            entity.setSeverity(patternSeed.severity());
            entity.setLanguage(patternSeed.language());
            entity.setActive(true);
            rumorPatternRepository.save(entity);
            inserted++;
        }
        log.info("Seeded rumor_patterns: inserted {} new rows.", inserted);
    }

    private void seedVerifiedClaims(List<VerifiedClaimSeed> claimSeeds) {
        int inserted = 0;
        for (VerifiedClaimSeed claimSeed : claimSeeds) {
            String normalizedClaim = TextNormalizer.normalize(claimSeed.claimText());
            Optional<VerifiedClaim> existing =
                    verifiedClaimRepository.findFirstByNormalizedClaimAndLanguage(normalizedClaim, claimSeed.language());
            if (existing.isPresent()) {
                continue;
            }

            VerifiedClaim claim = new VerifiedClaim();
            claim.setClaimText(claimSeed.claimText());
            claim.setNormalizedClaim(normalizedClaim);
            claim.setCategory(claimSeed.category());
            claim.setStatus(claimSeed.status());
            claim.setCorrectionText(claimSeed.correctionText());
            claim.setOfficialSource(
                    claimSeed.officialSource().isBlank()
                            ? "Demo Central Electoral Commission"
                            : claimSeed.officialSource()
            );
            claim.setOfficialSourceUrl(claimSeed.officialSourceUrl());
            claim.setLanguage(claimSeed.language());
            claim.setRegion(claimSeed.region() == null ? "General" : claimSeed.region());
            claim.setPublished(claimSeed.published());
            verifiedClaimRepository.save(claim);
            inserted++;
        }
        log.info("Seeded verified_claims: inserted {} new rows.", inserted);
    }

    private void cleanupSmsVotingClaims() {
        int updated = 0;
        List<VerifiedClaim> allClaims = verifiedClaimRepository.findAll();
        for (VerifiedClaim claim : allClaims) {
            String normalizedClaim = TextNormalizer.normalize(claim.getClaimText());
            if (!ElectionTextRules.isSmsVotingClaim(normalizedClaim)) {
                continue;
            }

            boolean changed = false;
            if (!Objects.equals(claim.getNormalizedClaim(), normalizedClaim)) {
                claim.setNormalizedClaim(normalizedClaim);
                changed = true;
            }
            if (claim.getStatus() != ClaimStatus.VERIFIED_FALSE) {
                claim.setStatus(ClaimStatus.VERIFIED_FALSE);
                changed = true;
            }
            if (!"fake_voting_method".equals(claim.getCategory())) {
                claim.setCategory("fake_voting_method");
                changed = true;
            }
            if (!Boolean.TRUE.equals(claim.getPublished())) {
                claim.setPublished(true);
                changed = true;
            }

            String correctionText = smsCorrectionForLanguage(claim.getLanguage());
            if (!Objects.equals(correctionText, claim.getCorrectionText())) {
                claim.setCorrectionText(correctionText);
                changed = true;
            }
            if (claim.getOfficialSource() == null || claim.getOfficialSource().isBlank()) {
                claim.setOfficialSource("Demo Central Electoral Commission");
                changed = true;
            }

            if (changed) {
                verifiedClaimRepository.save(claim);
                updated++;
            }
        }
        if (updated > 0) {
            log.info("Cleaned up {} SMS voting verified claims to VERIFIED_FALSE.", updated);
        }
    }

    private String smsCorrectionForLanguage(String language) {
        String normalizedLanguage = language == null ? "" : language.trim().toLowerCase(Locale.ROOT);
        return switch (normalizedLanguage) {
            case "ro" -> "Votarea prin SMS nu este disponibilă. Folosiți doar procedurile oficiale de vot.";
            case "ru" -> "Голосование по SMS недоступно. Используйте только официальные процедуры голосования.";
            default -> "Voting by SMS is not available. Use only official voting procedures.";
        };
    }

    private String blankToNull(String value) {
        String trimmed = value == null ? null : value.trim();
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }

    private enum SeedSection {
        NONE,
        OFFICIAL_INFO,
        RUMOR_PATTERNS,
        VERIFIED_CLAIMS,
        REQUIRED_DEMO_TESTS;

        static SeedSection fromHeader(String header) {
            return switch (header.toUpperCase(Locale.ROOT)) {
                case "## OFFICIAL_INFO" -> OFFICIAL_INFO;
                case "## RUMOR_PATTERNS" -> RUMOR_PATTERNS;
                case "## VERIFIED_CLAIMS" -> VERIFIED_CLAIMS;
                case "## REQUIRED DEMO TESTS" -> REQUIRED_DEMO_TESTS;
                default -> NONE;
            };
        }
    }

    private record OfficialSeed(
            String topic,
            String content,
            String sourceName,
            String sourceUrl,
            String language
    ) {
    }

    private record RumorPatternSeed(String phrase, String category, int severity, String language) {
    }

    private record VerifiedClaimSeed(
            String claimText,
            String category,
            ClaimStatus status,
            String correctionText,
            String officialSource,
            String officialSourceUrl,
            String language,
            String region,
            boolean published
    ) {
    }

    private record SeedData(
            List<OfficialSeed> officialInfo,
            List<RumorPatternSeed> rumorPatterns,
            List<VerifiedClaimSeed> verifiedClaims
    ) {
    }
}
